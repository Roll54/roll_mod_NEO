package com.roll_54.roll_mod.economy.vendingblock.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client &rarr; server request to create a listing. {@code item} already carries the desired count
 * (the picked item × the chosen quantity); the server verifies the player actually has them. For
 * fixed listings only {@code fixedPrice} matters; for bidding listings {@code startPrice} and
 * {@code buyout} (0 = none) are used.
 */
public record CreateListingPacket(
    ItemStack item, boolean bidding, long fixedPrice, long startPrice, long buyout, int days)
    implements CustomPacketPayload {

  public static final Type<CreateListingPacket> TYPE =
      new Type<>(RollMod.id("create_listing"));

  public static final StreamCodec<RegistryFriendlyByteBuf, CreateListingPacket> STREAM_CODEC =
      StreamCodec.composite(
          ItemStack.OPTIONAL_STREAM_CODEC,
          CreateListingPacket::item,
          ByteBufCodecs.BOOL,
          CreateListingPacket::bidding,
          ByteBufCodecs.VAR_LONG,
          CreateListingPacket::fixedPrice,
          ByteBufCodecs.VAR_LONG,
          CreateListingPacket::startPrice,
          ByteBufCodecs.VAR_LONG,
          CreateListingPacket::buyout,
          ByteBufCodecs.VAR_INT,
          CreateListingPacket::days,
          CreateListingPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(CreateListingPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (context.player() instanceof ServerPlayer player) {
            AuctionManager.createListing(
                player,
                packet.item(),
                packet.bidding(),
                packet.fixedPrice(),
                packet.startPrice(),
                packet.buyout(),
                packet.days());
          }
        });
  }
}
