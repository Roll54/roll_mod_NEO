package com.roll_54.roll_mod_client.compat.tei.tree;

import dev.lorp.tei.api.TeiDisplay;
import dev.lorp.tei.api.TeiSlot;
import dev.lorp.tei.client.FluidIngredients;
import dev.lorp.tei.client.ModIngredientStacks;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import dev.lorp.tei.fluid.TeiFluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One ingredient of the TEI handset tree: its alternatives (one stack, or every stack of a tag / list) and how much
 * of it is meant. Fluids are TEI's fluid stand-in stacks ({@link FluidIngredients#stack}), which TEI draws, describes
 * and looks up like the fluids of its item list; their amount is in mB. Alternatives are kept with a count of 1.
 */
public record TreeIngredient(List<ItemStack> stacks, long amount) {

    public TreeIngredient {
        stacks = List.copyOf(stacks);
    }

    /** One item, or a fluid stand-in. */
    public static TreeIngredient of(ItemStack stack, long amount) {
        return new TreeIngredient(List.of(stack.copyWithCount(1)), amount);
    }

    public static TreeIngredient of(Fluid fluid, long amount) {
        return new TreeIngredient(List.of(FluidIngredients.stack(fluid)), amount);
    }

    /**
     * What a recipe slot holds: its fluids (with their amount) when TEI reports fluids there, else its stacks.
     * Null for an empty slot and for other non-item ingredients (JEI mod types), which have no price either.
     */
    static @Nullable TreeIngredient ofSlot(TeiDisplay display, TeiSlot slot) {
        List<TeiFluidStack> fluids = null;
        try {
            fluids = display.fluidsAt(slot.x() + 1, slot.y() + 1);
        } catch (Throwable ignored) {
        }
        if (fluids != null && !fluids.isEmpty()) {
            List<ItemStack> stacks = new ArrayList<>();
            List<Fluid> seen = new ArrayList<>();
            for (TeiFluidStack f : fluids) {
                if (f.isEmpty() || seen.contains(f.getFluid())) continue;
                seen.add(f.getFluid());
                stacks.add(FluidIngredients.stack(f.getFluid()));
            }
            if (stacks.isEmpty()) return null;
            return new TreeIngredient(stacks, Math.max(1, fluids.get(0).getAmountLong()));
        }
        if (slot.stacks().isEmpty()) return null;
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack s : slot.stacks()) {
            if (s.isEmpty()) continue;
            if (ModIngredientStacks.isCarrier(s) && FluidIngredients.fluidOf(s) == null) return null;
            stacks.add(s.copyWithCount(1));
        }
        if (stacks.isEmpty()) return null;
        ItemStack first = slot.stacks().get(0);
        long amount = slot.amount() > 0 ? slot.amount() : Math.max(1, first.getCount());
        return new TreeIngredient(stacks, amount);
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public boolean single() {
        return stacks.size() == 1;
    }

    public ItemStack first() {
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0);
    }

    /** The alternative shown now, cycling once a second like TEI's own slots. */
    public ItemStack displayed() {
        if (stacks.isEmpty()) return ItemStack.EMPTY;
        return stacks.get((int) ((Util.getMillis() / 1000) % stacks.size()));
    }

    public boolean isFluid() {
        return !stacks.isEmpty() && fluidOf(stacks.get(0)) != null;
    }

    public TreeIngredient withAmount(long amount) {
        return new TreeIngredient(stacks, amount);
    }

    /** The stack TEI indexes recipes by: the stack itself (TEI now indexes a fluid by its own stand-in, not its bucket). */
    public static ItemStack lookupStack(ItemStack stack) {
        return stack;
    }

    static @Nullable Fluid fluidOf(ItemStack stack) {
        return FluidIngredients.fluidOf(stack);
    }

    /** What prices go by: the fluid, or the item (whatever data the stack carries). */
    static Object key(ItemStack stack) {
        Fluid fluid = fluidOf(stack);
        return fluid != null ? fluid : stack.getItem();
    }

    /** The same item or fluid; a bucket also stands for its fluid (TEI's own displays show fluids as buckets). */
    static boolean sameKey(ItemStack a, ItemStack b) {
        Object ka = key(a), kb = key(b);
        if (ka.equals(kb)) return true;
        return ka instanceof Fluid fa && b.getItem() instanceof BucketItem && fa.getBucket() == b.getItem()
                || kb instanceof Fluid fb && a.getItem() instanceof BucketItem && fb.getBucket() == a.getItem();
    }

    /** The same alternatives, in the same order (amounts aside). */
    public boolean sameAs(TreeIngredient other) {
        if (stacks.size() != other.stacks.size()) return false;
        for (int i = 0; i < stacks.size(); i++) {
            if (!key(stacks.get(i)).equals(key(other.stacks.get(i)))) return false;
        }
        return true;
    }

    /** Whether this could be {@code other} (they share an alternative). */
    public boolean mentions(TreeIngredient other) {
        for (ItemStack a : stacks) {
            for (ItemStack b : other.stacks) {
                if (sameKey(a, b)) return true;
            }
        }
        return false;
    }

    /** The server's key of a single stack: its item id, or {@code fluid:<id>}; null for a tag or list. */
    public @Nullable String priceKey() {
        if (!single()) return null;
        ItemStack stack = stacks.get(0);
        Fluid fluid = fluidOf(stack);
        if (fluid != null) return fluid == Fluids.EMPTY ? null : "fluid:" + BuiltInRegistries.FLUID.getKey(fluid);
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
