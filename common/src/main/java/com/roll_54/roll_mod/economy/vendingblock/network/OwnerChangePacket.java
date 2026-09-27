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

public record OwnerChangePacket(BlockPos pos, String newOwner) implements CustomPacketPayload {

  public static final Type<OwnerChangePacket> TYPE =
      new Type<>(RollMod.id("owner_change"));

  public static final StreamCodec<ByteBuf, OwnerChangePacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          OwnerChangePacket::pos,
          ByteBufCodecs.STRING_UTF8,
          OwnerChangePacket::newOwner,
          OwnerChangePacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(OwnerChangePacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          Level level = player.level();

          // Admin-only (admin UI opens only with the Vendor Key): reassigning the owner
          // hands over storage, payments and break rights.
          if (level.getBlockEntity(packet.pos()) instanceof VendorBlockEntity vendorBlockEntity
              && player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get())) {
            vendorBlockEntity.setOwnerByUsername(packet.newOwner());
          }
        });
  }
}
