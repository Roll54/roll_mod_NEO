package com.roll_54.roll_mod.economy.vendingblock.gui.components;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A {@link SlotItemHandler} whose <em>capacity</em> follows the handler's slot limit (1000) instead
 * of the item's vanilla max, so the vendor's bulk storage can be filled past 64 through the GUI. A
 * single pick-up onto the cursor is still capped at the item's vanilla stack size, so you take
 * items out in normal 64-stack chunks rather than grabbing 1000 onto the cursor at once.
 */
public class BigSlotItemHandler extends SlotItemHandler {

  public BigSlotItemHandler(IItemHandler handler, int index, int xPosition, int yPosition) {
    super(handler, index, xPosition, yPosition);
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    // capacity: how much may accumulate in the slot
    return getItemHandler().getSlotLimit(this.index);
  }

  @Override
  public ItemStack safeTake(int count, int limit, Player player) {
    // pick-up: never put more than one vanilla stack on the cursor at a time
    ItemStack inSlot = getItem();
    if (!inSlot.isEmpty()) count = Math.min(count, inSlot.getMaxStackSize());
    return super.safeTake(count, limit, player);
  }
}
