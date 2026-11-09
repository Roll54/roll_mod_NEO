package com.roll_54.roll_mod.compat.mi.machines.me;

import appeng.api.stacks.GenericStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.IntFunction;

/**
 * Read-only menu slots showing any AE2 stack, fluids and amounts past a stack included.
 *
 * <p>Each stack is shown as AE2's wrapped stack ({@link GenericStack#wrapInItemStack}); AE2 hooks
 * {@code GuiGraphics.renderItem} to draw it as its item or fluid with a compact amount, so any slot can show
 * it. The server reads {@code source}; the client keeps what the vanilla menu sync sends it. Nothing can be
 * put in or taken out.
 */
public final class GenericStackDisplayHandler implements IItemHandlerModifiable {

    private final @Nullable IntFunction<GenericStack> source;
    private final ItemStack[] shown;

    /**
     * @param source the stack of each slot on the server ({@code null} when empty); {@code null} on the
     *               client, which shows what the server syncs
     */
    public GenericStackDisplayHandler(int slots, @Nullable IntFunction<GenericStack> source) {
        this.source = source;
        this.shown = new ItemStack[slots];
        Arrays.fill(shown, ItemStack.EMPTY);
    }

    /** The AE2 stack shown in {@code slot}, on either side. */
    public @Nullable GenericStack getShown(int slot) {
        return GenericStack.unwrapItemStack(getStackInSlot(slot));
    }

    @Override
    public int getSlots() {
        return shown.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (source == null) return shown[slot];
        GenericStack stack = source.apply(slot);
        return stack == null || stack.amount() <= 0 ? ItemStack.EMPTY : GenericStack.wrapInItemStack(stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        // The client's copy, from the menu sync. (The server's slots also pass through here when the menu
        // probes stack sizes, which changes nothing: the server reads source.)
        shown[slot] = stack;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }
}
