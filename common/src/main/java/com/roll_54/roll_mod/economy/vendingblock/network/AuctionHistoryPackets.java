package com.roll_54.roll_mod.economy.vendingblock.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.repository.AuctionHistory;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionHistoryEntry;
import com.roll_54.roll_mod.economy.vendingblock.client.ClientAuctionHistoryCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The Auction House history's two packets: the client asks, the server answers with the newest
 * rows of {@code auction_history}. Shaped like the money history's {@code HistoryPackets}.
 */
public final class AuctionHistoryPackets {

  /** Rows per answer: the newest, which is what anyone scrolls a history for. */
  private static final int LIMIT = 100;
  /** A history request is a database query; one per player per half second is plenty. */
  private static final long MIN_INTERVAL_MILLIS = 500L;
  private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

  private AuctionHistoryPackets() {}

  /** Client → server: send me my auction history. */
  public record Request() implements CustomPacketPayload {

    public static final Type<Request> TYPE = new Type<>(RollMod.id("auction_history_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC =
        StreamCodec.unit(new Request());

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }

    public static void handle(Request payload, IPayloadContext context) {
      context.enqueueWork(() -> {
        if (!(context.player() instanceof ServerPlayer player)) return;
        long now = System.currentTimeMillis();
        Long last = LAST_REQUEST.put(player.getUUID(), now);
        if (last != null && now - last < MIN_INTERVAL_MILLIS) return;

        MinecraftServer server = player.server;
        UUID id = player.getUUID();
        AuctionHistory.history(id, LIMIT)
            .thenAccept(rows -> server.execute(() -> {
              ServerPlayer online = server.getPlayerList().getPlayer(id);
              if (online != null) PacketDistributor.sendToPlayer(online, new Sync(rows));
            }));
      });
    }
  }

  /** Server → client: the rows, newest first. */
  public record Sync(List<AuctionHistoryEntry> entries) implements CustomPacketPayload {

    public static final Type<Sync> TYPE = new Type<>(RollMod.id("auction_history_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
          int count = Math.min(LIMIT, payload.entries().size());
          buf.writeVarInt(count);
          for (int i = 0; i < count; i++) payload.entries().get(i).encode(buf);
        },
        buf -> {
          int count = Math.min(LIMIT, buf.readVarInt());
          List<AuctionHistoryEntry> entries = new ArrayList<>(count);
          for (int i = 0; i < count; i++) entries.add(AuctionHistoryEntry.decode(buf));
          return new Sync(List.copyOf(entries));
        });

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }

    public static void handle(Sync payload, IPayloadContext context) {
      context.enqueueWork(() -> {
        ClientAuctionHistoryCache.ENTRIES = payload.entries();
        ClientAuctionHistoryCache.LOADED = true;
        ClientAuctionHistoryCache.VERSION++;
      });
    }
  }
}
