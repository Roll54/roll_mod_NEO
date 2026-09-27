package com.roll_54.roll_mod.economy.vendingblock.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DiscardsPaymentPacket(BlockPos pos, boolean discardsPayment)
    implements CustomPacketPayload {

  public static final Type<DiscardsPaymentPacket> TYPE =
      new Type<>(RollMod.id("discards_payment"));

  public static final StreamCodec<ByteBuf, DiscardsPaymentPacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          DiscardsPaymentPacket::pos,
          ByteBufCodecs.BOOL,
          DiscardsPaymentPacket::discardsPayment,
          DiscardsPaymentPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(DiscardsPaymentPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          Level level = player.level();

          // Admin-only (admin UI opens only with the Vendor Key): discarding voids the
          // owner's revenue / the sold items.
          if (level.getBlockEntity(packet.pos()) instanceof VendorBlockEntity vendorBlockEntity
              && player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get())) {
            vendorBlockEntity.setDiscarding(packet.discardsPayment());
          }
        });
  }
}
