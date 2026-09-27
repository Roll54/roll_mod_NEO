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

public record InfiniteInventoryPacket(BlockPos pos, boolean infiniteInventory)
    implements CustomPacketPayload {

  public static final Type<InfiniteInventoryPacket> TYPE =
      new Type<>(RollMod.id("infinite_inventory"));

  public static final StreamCodec<ByteBuf, InfiniteInventoryPacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          InfiniteInventoryPacket::pos,
          ByteBufCodecs.BOOL,
          InfiniteInventoryPacket::infiniteInventory,
          InfiniteInventoryPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(InfiniteInventoryPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          Level level = player.level();

          // Admin-only toggle (the admin UI opens only with the Vendor Key): infinite
          // inventory mints items/money, so never trust the packet alone.
          if (level.getBlockEntity(packet.pos()) instanceof VendorBlockEntity vendorBlockEntity
              && player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get())) {
            vendorBlockEntity.setInfinite(packet.infiniteInventory());
          }
        });
  }
}
