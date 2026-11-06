package com.roll_54.roll_mod.economy.minprice;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A fluid container's two parts for its minimal price: the container itself and the fluid in it. A
 * filled bucket is the empty bucket plus one bucket of its fluid; an MI tank (or any item with a fluid
 * handler) is the tank plus what it holds. Server check and client tooltip both price it this way.
 */
public final class FluidContents {

  private FluidContents() {}

  /** The fluid one of {@code stack} holds, or null when it holds none. */
  public static @Nullable FluidStack of(ItemStack stack) {
    if (stack.isEmpty()) return null;
    try {
      FluidStack fluid = FluidUtil.getFluidContained(stack.copyWithCount(1)).orElse(FluidStack.EMPTY);
      return fluid.isEmpty() ? null : fluid;
    } catch (RuntimeException e) {
      return null; // a broken fluid handler should not break a tooltip or a sale
    }
  }

  /** The item key the container part is priced as: the empty bucket for a filled bucket, else itself. */
  public static String containerKey(ItemStack stack) {
    if (stack.getItem() instanceof BucketItem && stack.getItem() != Items.BUCKET) {
      return RecipeSnapshot.itemKey(Items.BUCKET);
    }
    return RecipeSnapshot.itemKey(stack.getItem());
  }

  /** The table key of the fluid ({@code fluid:<id>}). */
  public static String fluidKey(FluidStack fluid) {
    return PriceSolver.FLUID_PREFIX + BuiltInRegistries.FLUID.getKey(fluid.getFluid());
  }

  /** How many buckets of it there are. */
  public static double buckets(FluidStack fluid) {
    return fluid.getAmount() / 1000.0;
  }
}
