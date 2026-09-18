package com.roll_54.roll_mod.economy.vendingblock.blockentity.transaction;

import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class VendorBlockInventory {

  /** Whether {@code product} (with its count) fits into the buyer's inventory. */
  public static boolean checkInventorySpace(Player buyer, ItemStack product) {
    int remaining = product.getCount();
    for (int i = 0; i < 36 && remaining > 0; i++) {
      ItemStack slot = buyer.getInventory().getItem(i);
      if (slot.isEmpty()) {
        remaining -= Math.min(remaining, product.getMaxStackSize());
      } else if (ItemStack.isSameItemSameComponents(slot, product)) {
        remaining -= Math.min(remaining, slot.getMaxStackSize() - slot.getCount());
      }
    }
    return remaining <= 0;
  }

  /** Units of {@code item} available in the vendor's bulk storage. */
  public static int stockCount(VendorBlockEntity vendor, ItemStack item) {
    return vendor.storage.countOf(item);
  }

  /** Remove up to {@code amount} units of {@code item} from storage; returns the amount removed. */
  public static int deductFromStorage(VendorBlockEntity vendor, ItemStack item, int amount) {
    if (vendor.isInfinite()) return amount;
    return vendor.storage.remove(item, amount);
  }

  /** Units of {@code item} (same item + components) held in the seller's inventory. */
  public static int countInPlayer(Player seller, ItemStack item) {
    if (item.isEmpty()) return 0;
    int total = 0;
    for (int i = 0; i < 36; i++) {
      ItemStack slot = seller.getInventory().getItem(i);
      if (ItemStack.isSameItemSameComponents(slot, item)) total += slot.getCount();
    }
    return total;
  }

  /**
   * Remove up to {@code amount} units of {@code item} from the seller; returns the amount removed.
   */
  public static int removeFromPlayer(Player seller, ItemStack item, int amount) {
    if (item.isEmpty() || amount <= 0) return 0;
    int removed = 0;
    for (int i = 0; i < 36 && removed < amount; i++) {
      ItemStack slot = seller.getInventory().getItem(i);
      if (!ItemStack.isSameItemSameComponents(slot, item)) continue;
      int take = Math.min(amount - removed, slot.getCount());
      slot.shrink(take);
      removed += take;
    }
    return removed;
  }
}
