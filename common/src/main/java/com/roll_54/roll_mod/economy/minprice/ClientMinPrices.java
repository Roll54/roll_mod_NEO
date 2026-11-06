package com.roll_54.roll_mod.economy.minprice;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The client's copy of the minimal price table, as the last {@link SyncMinPricesPacket} had it.
 * Read by the item tooltip and by the JEI fluid tooltip.
 */
public final class ClientMinPrices {

  /** Item id → minimal price of one item. Filled buckets are not in it. */
  public static volatile Map<String, Double> ITEMS = Map.of();

  /** Fluid id → minimal price of one bucket (1000 mB). */
  public static volatile Map<String, Double> FLUIDS = Map.of();

  /** Absolute minimal price of one item / one bucket; 0 while the server sent no table. */
  public static volatile double MINIMUM = 0;

  /** Same depth the server looks into containers with ({@link MinPriceService}). */
  private static final int CONTENTS_DEPTH = 4;

  private ClientMinPrices() {}

  public static void clear() {
    ITEMS = Map.of();
    FLUIDS = Map.of();
    MINIMUM = 0;
  }

  /**
   * Price of one of {@code stack}, contents included, the way the server checks it; {@code 0} when
   * it has no floor.
   */
  public static double unit(ItemStack stack) {
    return unit(ITEMS, stack, CONTENTS_DEPTH);
  }

  private static double unit(Map<String, Double> items, ItemStack stack, int depth) {
    if (stack.isEmpty()) return 0;
    Double own = items.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    double total = Math.max(own == null ? 0 : own, MINIMUM);
    if (depth <= 0) return total;
    ItemContainerContents container = stack.get(DataComponents.CONTAINER);
    if (container != null) {
      for (ItemStack in : container.nonEmptyItems()) total += unit(items, in, depth - 1) * in.getCount();
    }
    BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
    if (bundle != null) {
      for (ItemStack in : bundle.items()) total += unit(items, in, depth - 1) * in.getCount();
    }
    return total;
  }

  /** The tooltip line for a fluid, or {@code null} when it has no floor. */
  public static Component fluidLine(String fluidId) {
    Double known = FLUIDS.get(fluidId);
    double price = Math.max(known == null ? 0 : known, MINIMUM);
    if (price <= 0) return null;
    return Component.translatable("tooltip.roll_mod.minprice.fluid", format(price));
  }

  /** Whole numbers when it is at least 1, two decimals below that; always rounded up, like the check. */
  public static String format(double price) {
    if (price >= 1) return String.format("%,d", (long) Math.ceil(price - 1e-9));
    return String.format(java.util.Locale.ROOT, "%.2f", Math.ceil(price * 100 - 1e-6) / 100);
  }
}
