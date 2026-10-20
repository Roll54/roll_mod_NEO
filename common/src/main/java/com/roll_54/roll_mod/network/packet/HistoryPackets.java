package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.ledger.HistoryFilter;
import com.roll_54.roll_mod.economy.currency.ledger.LedgerEntry;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyLedger;
import com.roll_54.roll_mod.minestar.hub.gui.ClientHistoryCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The money history's two packets: the client asks with a filter, the server answers with rows. */
public final class HistoryPackets {

    /** Rows per answer: the newest, which is what anyone scrolls a history for. */
    private static final int LIMIT = 100;
    /** A history request is a database query; one per player per half second is plenty. */
    private static final long MIN_INTERVAL_MILLIS = 500L;
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    private HistoryPackets() {}

    private static HistoryFilter filterOf(int ordinal) {
        // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
        return HistoryFilter.values()[Math.floorMod(ordinal, HistoryFilter.values().length)];
    }

    /** Client → server: send me my history, as this filter shows it. */
    public record Request(HistoryFilter filter) implements CustomPacketPayload {

        public static final Type<Request> TYPE = new Type<>(RollMod.id("history_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeVarInt(payload.filter().ordinal()),
                buf -> new Request(filterOf(buf.readVarInt())));

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
                CurrencyLedger.history(id, CurrencyType.MAIN, payload.filter(), LIMIT)
                        .thenAccept(rows -> server.execute(() -> {
                            ServerPlayer online = server.getPlayerList().getPlayer(id);
                            if (online != null) {
                                PacketDistributor.sendToPlayer(online, new Sync(payload.filter(), rows));
                            }
                        }));
            });
        }
    }

    /** Server → client: the rows, newest first. */
    public record Sync(HistoryFilter filter, List<LedgerEntry> entries) implements CustomPacketPayload {

        public static final Type<Sync> TYPE = new Type<>(RollMod.id("history_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.filter().ordinal());
                    int count = Math.min(LIMIT, payload.entries().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) payload.entries().get(i).encode(buf);
                },
                buf -> {
                    HistoryFilter filter = filterOf(buf.readVarInt());
                    int count = Math.min(LIMIT, buf.readVarInt());
                    List<LedgerEntry> entries = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) entries.add(LedgerEntry.decode(buf));
                    return new Sync(filter, List.copyOf(entries));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sync payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ClientHistoryCache.FILTER = payload.filter();
                ClientHistoryCache.ENTRIES = payload.entries();
                ClientHistoryCache.LOADED = true;
                ClientHistoryCache.VERSION++;
            });
        }
    }
}
