package com.roll_54.roll_mod.economy.vendingblock.gui.components;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * The vending block's ME interface storage as item slots, for the owner's screen: the same slots AE2's
 * interface screen shows, so items go in and out by hand and the interface's slot filter (its config)
 * still applies. Non-item stacks (fluids) show as empty and cannot be touched here.
 */
public class InterfaceStorageHandler implements IItemHandlerModifiable {

  private final GenericStackInv storage;

  public InterfaceStorageHandler(GenericStackInv storage) {
    this.storage = storage;
  }

  @Override
  public int getSlots() {
    return storage.size();
  }

  @Override
  public ItemStack getStackInSlot(int slot) {
    GenericStack stack = storage.getStack(slot);
    return stack != null && stack.what() instanceof AEItemKey key
        ? key.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount()))
        : ItemStack.EMPTY;
  }

  @Override
  public void setStackInSlot(int slot, ItemStack stack) {
    GenericStack current = storage.getStack(slot);
    if (current != null && !(current.what() instanceof AEItemKey)) return;
    storage.setStack(slot, stack.isEmpty() ? null : GenericStack.fromItemStack(stack));
  }

  @Override
  public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    AEItemKey key = AEItemKey.of(stack);
    if (key == null) return stack;
    long inserted =
        storage.insert(
            slot, key, stack.getCount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE);
    return inserted <= 0 ? stack : stack.copyWithCount(stack.getCount() - (int) inserted);
  }

  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    ItemStack inSlot = getStackInSlot(slot);
    if (inSlot.isEmpty() || amount <= 0) return ItemStack.EMPTY;
    long taken =
        storage.extract(
            slot,
            AEItemKey.of(inSlot),
            Math.min(amount, inSlot.getMaxStackSize()),
            simulate ? Actionable.SIMULATE : Actionable.MODULATE);
    return taken <= 0 ? ItemStack.EMPTY : inSlot.copyWithCount((int) taken);
  }

  @Override
  public int getSlotLimit(int slot) {
    return 64;
  }

  @Override
  public boolean isItemValid(int slot, ItemStack stack) {
    AEItemKey key = AEItemKey.of(stack);
    return key != null && storage.isAllowedIn(slot, key);
  }
}
