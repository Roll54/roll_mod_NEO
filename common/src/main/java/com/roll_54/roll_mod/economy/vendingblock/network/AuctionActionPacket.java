package com.roll_54.roll_mod.economy.vendingblock.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionManager;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client &rarr; server action on a listing, addressed by its UUID (so it is independent of any
 * client-side filtering/pagination of the grid). {@code amount} is only used by {@link Action#BID};
 * {@code listingId} is ignored by {@link Action#CLAIM}.
 */
public record AuctionActionPacket(Action action, UUID listingId, long amount)
    implements CustomPacketPayload {

  public enum Action {
    BUY,
    BID,
    BUYOUT,
    CANCEL,
    CLAIM;

    private static final Action[] VALUES = values();
  }

  public static final Type<AuctionActionPacket> TYPE =
      new Type<>(RollMod.id("auction_action"));

  public static final StreamCodec<RegistryFriendlyByteBuf, AuctionActionPacket> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.VAR_INT.map(i -> Action.VALUES[i], a -> a.ordinal()),
          AuctionActionPacket::action,
          UUIDUtil.STREAM_CODEC,
          AuctionActionPacket::listingId,
          ByteBufCodecs.VAR_LONG,
          AuctionActionPacket::amount,
          AuctionActionPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static AuctionActionPacket of(Action action, UUID listingId) {
    return new AuctionActionPacket(action, listingId, 0L);
  }

  public static AuctionActionPacket claim() {
    return new AuctionActionPacket(Action.CLAIM, net.minecraft.Util.NIL_UUID, 0L);
  }

  public static void handle(AuctionActionPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof ServerPlayer player)) return;
          switch (packet.action()) {
            case BUY -> AuctionManager.buyFixed(player, packet.listingId());
            case BID -> AuctionManager.placeBid(player, packet.listingId(), packet.amount());
            case BUYOUT -> AuctionManager.buyout(player, packet.listingId());
            case CANCEL -> AuctionManager.cancel(player, packet.listingId());
            case CLAIM -> AuctionManager.claimAll(player);
          }
        });
  }
}
