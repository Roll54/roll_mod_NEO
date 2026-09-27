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

public record BuyModePacket(BlockPos pos, boolean buyMode) implements CustomPacketPayload {

  public static final Type<BuyModePacket> TYPE =
      new Type<>(RollMod.id("buy_mode"));

  public static final StreamCodec<ByteBuf, BuyModePacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          BuyModePacket::pos,
          ByteBufCodecs.BOOL,
          BuyModePacket::buyMode,
          BuyModePacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(BuyModePacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          Level level = player.level();

          // Owner/key only: buy mode also unlocks hopper extraction of position items
          // (VendorBlockEntity.extractItemHandler), so a stranger flipping it could drain
          // a shop's sale stock.
          if (level.getBlockEntity(packet.pos()) instanceof VendorBlockEntity vendorBlockEntity
              && (vendorBlockEntity.isOwner(player)
                  || player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get()))) {
            vendorBlockEntity.setBuyMode(packet.buyMode());
          }
        });
  }
}
