package com.roll_54.roll_mod.economy.vendingblock.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.Position;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AddPositionPacket(BlockPos pos, ItemStack item, int amount, long price)
    implements CustomPacketPayload {

  public static final Type<AddPositionPacket> TYPE =
      new Type<>(RollMod.id("add_position"));

  public static final StreamCodec<RegistryFriendlyByteBuf, AddPositionPacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          AddPositionPacket::pos,
          ItemStack.OPTIONAL_STREAM_CODEC,
          AddPositionPacket::item,
          ByteBufCodecs.VAR_INT,
          AddPositionPacket::amount,
          ByteBufCodecs.VAR_LONG,
          AddPositionPacket::price,
          AddPositionPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(AddPositionPacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          Level level = player.level();
          if (level.getBlockEntity(packet.pos()) instanceof VendorBlockEntity vendor
              && (vendor.isOwner(player)
                  || player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get()))
              && !packet.item().isEmpty()) {
            vendor.addPosition(new Position(packet.item(), packet.amount(), packet.price()));
          }
        });
  }
}
