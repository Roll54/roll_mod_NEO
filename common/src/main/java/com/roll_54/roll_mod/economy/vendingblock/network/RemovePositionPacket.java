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

public record RemovePositionPacket(BlockPos pos, int index) implements CustomPacketPayload {

  public static final Type<RemovePositionPacket> TYPE =
      new Type<>(RollMod.id("remove_position"));

  public static final StreamCodec<ByteBuf, RemovePositionPacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          RemovePositionPacket::pos,
          ByteBufCodecs.VAR_INT,
          RemovePositionPacket::index,
          RemovePositionPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(RemovePositionPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          Level level = player.level();
          if (level.getBlockEntity(packet.pos()) instanceof VendorBlockEntity vendor
              && (vendor.isOwner(player)
                  || player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get()))) {
            vendor.removePosition(packet.index());
          }
        });
  }
}
