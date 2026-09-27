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
            // The UI only ever edits the facade filter (slot 1) with a single validated
            // item; slot 0 with count > 1 would drop real item entities, and other
            // indices throw. Re-run the client-side checks here — never trust the packet.
            if (packet.slotIndex() != 1) return;
            if (!displayBlockEntity.isOwner(player)
                && !player.getMainHandItem()
                    .is(com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry
                        .VENDOR_KEY.get())) {
              return;
            }
            ItemStack stack = packet.stack();
            if (!stack.isEmpty()) {
              if (com.roll_54.roll_mod.economy.vendingblock.gui.components.FilterValidation
                      .isBlacklistedFacade(stack, level, packet.pos())
                  || !com.roll_54.roll_mod.economy.vendingblock.gui.components.FilterValidation
                      .isFullBlock(stack, level, packet.pos())) {
                return;
              }
              stack = stack.copyWithCount(1);
            }
            displayBlockEntity.inventory.setStackInSlot(packet.slotIndex(), stack);
            displayBlockEntity.setChanged();
          }
        });
  }
}
