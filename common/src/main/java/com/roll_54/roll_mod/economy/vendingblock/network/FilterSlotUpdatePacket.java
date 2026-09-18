package com.roll_54.roll_mod.economy.vendingblock.network;

import com.roll_54.roll_mod.RollMod;
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

public record FilterSlotUpdatePacket(BlockPos pos, int slotIndex, ItemStack stack)
    implements CustomPacketPayload {

  public static final Type<FilterSlotUpdatePacket> TYPE =
      new Type<>(RollMod.id("filter_slot_update"));

  public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_ITEM_STACK_CODEC =
      StreamCodec.of(
          (buf, stack) -> {
            if (stack.isEmpty()) {
              buf.writeBoolean(false);
            } else {
              buf.writeBoolean(true);
              ItemStack.STREAM_CODEC.encode(buf, stack);
            }
          },
          (buf) -> {
            if (buf.readBoolean()) {
              return ItemStack.STREAM_CODEC.decode(buf);
            } else {
              return ItemStack.EMPTY;
            }
          });

  public static final StreamCodec<RegistryFriendlyByteBuf, FilterSlotUpdatePacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          FilterSlotUpdatePacket::pos,
          ByteBufCodecs.VAR_INT,
          FilterSlotUpdatePacket::slotIndex,
          OPTIONAL_ITEM_STACK_CODEC,
          FilterSlotUpdatePacket::stack,
          FilterSlotUpdatePacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(FilterSlotUpdatePacket packet, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          Level level = player.level();

          if (level.getBlockEntity(packet.pos())
              instanceof
              com.roll_54.roll_mod.economy.vendingblock.blockentity.DisplayBlockEntity
                  displayBlockEntity) {
            displayBlockEntity.inventory.setStackInSlot(packet.slotIndex(), packet.stack());
            displayBlockEntity.setChanged();
          }
        });
  }
}
