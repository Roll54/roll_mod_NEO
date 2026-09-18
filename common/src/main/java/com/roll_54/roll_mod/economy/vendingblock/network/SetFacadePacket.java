package com.roll_54.roll_mod.economy.vendingblock.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.components.FilterValidation;
import com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetFacadePacket(BlockPos pos, ItemStack facade) implements CustomPacketPayload {

  public static final Type<SetFacadePacket> TYPE =
      new Type<>(RollMod.id("set_facade"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SetFacadePacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          SetFacadePacket::pos,
          ItemStack.OPTIONAL_STREAM_CODEC,
          SetFacadePacket::facade,
          SetFacadePacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(SetFacadePacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          Level level = player.level();
          if (level.getBlockEntity(packet.pos()) instanceof VendorBlockEntity vendor
              && (vendor.isOwner(player)
                  || player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get()))) {
            ItemStack facade = packet.facade();
            if (facade.isEmpty()) {
              vendor.setFacade(ItemStack.EMPTY);
            } else if (!FilterValidation.isBlacklistedFacade(facade, level, packet.pos())
                && FilterValidation.isFullBlock(facade, level, packet.pos())) {
              vendor.setFacade(facade);
            }
          }
        });
  }
}
