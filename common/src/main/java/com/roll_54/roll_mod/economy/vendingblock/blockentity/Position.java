package com.roll_54.roll_mod.economy.vendingblock.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * A single sale entry in a vendor block.
 *
 * @param item prototype of the item being sold (count is ignored/normalised to 1; components
 *     matter)
 * @param amount how many units are given to the buyer per purchase (>= 1)
 * @param price price in currency units (0 = free)
 */
public record Position(ItemStack item, int amount, long price) {

  public Position {
    amount = Math.max(1, amount);
    price = Math.max(0L, price);
  }

  /** The item stack shown to the buyer (one purchase worth). */
  public ItemStack displayStack() {
    return item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(amount);
  }

  public CompoundTag toTag(HolderLookup.Provider registries) {
    CompoundTag tag = new CompoundTag();
    tag.put("item", item.copyWithCount(1).saveOptional(registries));
    tag.putInt("amount", amount);
    tag.putLong("price", price);
    return tag;
  }

  public static Position fromTag(CompoundTag tag, HolderLookup.Provider registries) {
    ItemStack item = ItemStack.parseOptional(registries, tag.getCompound("item"));
    return new Position(item, tag.getInt("amount"), tag.getLong("price"));
  }
}
