package com.roll_54.roll_mod.economy.service.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.service.ServiceBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The owner's new price and batch size for a service block. */
public record ServiceSettingsPacket(BlockPos pos, long price, long batch) implements CustomPacketPayload {

  public static final Type<ServiceSettingsPacket> TYPE = new Type<>(RollMod.id("service_settings"));

  public static final StreamCodec<ByteBuf, ServiceSettingsPacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          ServiceSettingsPacket::pos,
          ByteBufCodecs.VAR_LONG,
          ServiceSettingsPacket::price,
          ByteBufCodecs.VAR_LONG,
          ServiceSettingsPacket::batch,
          ServiceSettingsPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(ServiceSettingsPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          // Owner or vendor key only; never a negative price or a batch under 1.
          if (packet.price() >= 0
              && packet.batch() >= 1
              && player.level().getBlockEntity(packet.pos()) instanceof ServiceBlockEntity be
              && (be.isOwner(player) || player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get()))) {
            be.setPrice(packet.price());
            be.setBatch(packet.batch());
          }
        });
  }
}
