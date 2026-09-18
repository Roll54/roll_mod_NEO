package com.roll_54.roll_mod.economy.vendingblock.blockentity;

import java.util.Arrays;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Chest-sized bulk storage for the vendor block. Each slot holds up to {@link #CAP} of one item and
 * can be locked to a specific item (so only that item may enter, including via hoppers).
 *
 * <p>Counts can exceed the vanilla 99-per-stack NBT limit, so this does NOT serialize via {@code
 * ItemStack.CODEC} (which clamps count to 1..99). Instead each slot persists a prototype stack
 * (count 1) plus a separate {@code int count}. Network/menu sync is fine because {@code
 * ItemStack.STREAM_CODEC} writes count as an unclamped VarInt.
 */
public class VendorStorage implements IItemHandlerModifiable {

  public static final int SLOTS = 27;
  public static final int CAP = 1000;

  private final ItemStack[] stacks = new ItemStack[SLOTS]; // in-memory, count up to CAP
  private final boolean[] locked = new boolean[SLOTS];
  private final ItemStack[] lockFilter = new ItemStack[SLOTS]; // count 1, the locked item
  private final Runnable onChanged;

  public VendorStorage(Runnable onChanged) {
    this.onChanged = onChanged;
    Arrays.fill(stacks, ItemStack.EMPTY);
    Arrays.fill(lockFilter, ItemStack.EMPTY);
  }

  @Override
  public int getSlots() {
    return SLOTS;
  }

  @Override
  public ItemStack getStackInSlot(int slot) {
    return stacks[slot];
  }

  @Override
  public int getSlotLimit(int slot) {
    return CAP;
  }

  @Override
  public boolean isItemValid(int slot, ItemStack stack) {
    if (stack.isEmpty()) return false;
    if (locked[slot]
        && !lockFilter[slot].isEmpty()
        && !ItemStack.isSameItemSameComponents(stack, lockFilter[slot])) {
      return false;
    }
    ItemStack existing = stacks[slot];
    return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack);
  }

  @Override
  public void setStackInSlot(int slot, ItemStack stack) {
    if (stack.getCount() > CAP) stack = stack.copyWithCount(CAP);
    stacks[slot] = stack;
    changed(slot);
  }

  @Override
  public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
    ItemStack existing = stacks[slot];
    int limit = CAP - (existing.isEmpty() ? 0 : existing.getCount());
    if (limit <= 0) return stack;
    int toAdd = Math.min(limit, stack.getCount());
    if (!simulate) {
      if (existing.isEmpty()) {
        stacks[slot] = stack.copyWithCount(toAdd);
      } else {
        stacks[slot] = existing.copyWithCount(existing.getCount() + toAdd);
      }
      changed(slot);
    }
    int remaining = stack.getCount() - toAdd;
    return remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
  }

  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    if (amount <= 0) return ItemStack.EMPTY;
    ItemStack existing = stacks[slot];
    if (existing.isEmpty()) return ItemStack.EMPTY;
    int toExtract = Math.min(amount, existing.getCount());
    ItemStack extracted = existing.copyWithCount(toExtract);
    if (!simulate) {
      int left = existing.getCount() - toExtract;
      stacks[slot] = left <= 0 ? ItemStack.EMPTY : existing.copyWithCount(left);
      changed(slot);
    }
    return extracted;
  }

  // --- locking ---------------------------------------------------------

  public boolean isLocked(int slot) {
    return locked[slot];
  }

  public ItemStack getLockFilter(int slot) {
    return lockFilter[slot];
  }

  public void setLocked(int slot, boolean lock) {
    locked[slot] = lock;
    if (lock) {
      // lock to the current item if any; otherwise capture the first item inserted later
      lockFilter[slot] = stacks[slot].isEmpty() ? ItemStack.EMPTY : stacks[slot].copyWithCount(1);
    } else {
      lockFilter[slot] = ItemStack.EMPTY;
    }
    changed(slot);
  }

  private void changed(int slot) {
    if (stacks[slot].isEmpty()) {
      if (!locked[slot]) lockFilter[slot] = ItemStack.EMPTY; // unlocked empty slot forgets its item
    } else if (locked[slot] && lockFilter[slot].isEmpty()) {
      lockFilter[slot] = stacks[slot].copyWithCount(1); // locked empty slot captures its first item
    }
    if (onChanged != null) onChanged.run();
  }

  /** Total number of units of {@code match} held across all slots. */
  public int countOf(ItemStack match) {
    if (match.isEmpty()) return 0;
    int total = 0;
    for (ItemStack stack : stacks) {
      if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, match)) {
        total += stack.getCount();
      }
    }
    return total;
  }

  /** Remove up to {@code amount} units of {@code match}; returns how many were actually removed. */
  public int remove(ItemStack match, int amount) {
    if (match.isEmpty() || amount <= 0) return 0;
    int removed = 0;
    for (int i = 0; i < SLOTS && removed < amount; i++) {
      ItemStack stack = stacks[i];
      if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, match)) continue;
      int take = Math.min(amount - removed, stack.getCount());
      int left = stack.getCount() - take;
      stacks[i] = left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
      removed += take;
      changed(i);
    }
    return removed;
  }

  // --- persistence -----------------------------------------------------

  public CompoundTag serializeNBT(HolderLookup.Provider registries) {
    ListTag list = new ListTag();
    for (int i = 0; i < SLOTS; i++) {
      CompoundTag slotTag = new CompoundTag();
      slotTag.putInt("slot", i);
      ItemStack proto = stacks[i].isEmpty() ? ItemStack.EMPTY : stacks[i].copyWithCount(1);
      slotTag.put("item", proto.saveOptional(registries));
      slotTag.putInt("count", stacks[i].getCount());
      slotTag.putBoolean("locked", locked[i]);
      slotTag.put("filter", lockFilter[i].saveOptional(registries));
      list.add(slotTag);
    }
    CompoundTag tag = new CompoundTag();
    tag.put("slots", list);
    return tag;
  }

  public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
    Arrays.fill(stacks, ItemStack.EMPTY);
    Arrays.fill(lockFilter, ItemStack.EMPTY);
    Arrays.fill(locked, false);
    ListTag list = tag.getList("slots", 10); // 10 = CompoundTag
    for (int i = 0; i < list.size(); i++) {
      CompoundTag slotTag = list.getCompound(i);
      int slot = slotTag.getInt("slot");
      if (slot < 0 || slot >= SLOTS) continue;
      ItemStack proto = ItemStack.parseOptional(registries, slotTag.getCompound("item"));
      int count = Math.min(slotTag.getInt("count"), CAP);
      stacks[slot] = proto.isEmpty() || count <= 0 ? ItemStack.EMPTY : proto.copyWithCount(count);
      locked[slot] = slotTag.getBoolean("locked");
      lockFilter[slot] = ItemStack.parseOptional(registries, slotTag.getCompound("filter"));
    }
  }
}
