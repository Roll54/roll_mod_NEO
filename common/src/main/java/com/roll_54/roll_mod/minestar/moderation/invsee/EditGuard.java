package com.roll_54.roll_mod.minestar.moderation.invsee;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

/**
 * Puts one gate and one record in front of a handler the window edits. Writes only go through
 * while {@code writable} says so: the moderator may edit, and the player is still where the session
 * found them. After every write that changed something, {@code after} runs (an offline save) and
 * {@code edits} hears about it.
 *
 * <p>Reads are never gated, so a read-only window still shows everything live.
 */
final class EditGuard implements IItemHandlerModifiable {

    private final IItemHandlerModifiable delegate;
    private final BooleanSupplier writable;
    private final IntFunction<String> where;
    private final InvSeeEdit edits;
    private final Runnable after;

    EditGuard(IItemHandlerModifiable delegate, BooleanSupplier writable, IntFunction<String> where,
              InvSeeEdit edits, Runnable after) {
        this.delegate = delegate;
        this.writable = writable;
        this.where = where;
        this.edits = edits;
        this.after = after;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    /**
     * A copy, never the live stack. LdLib2's quick-move and vanilla's {@code safeInsert} shrink or
     * grow what {@code Slot.getItem()} returns in place, then write it back. With the live stack
     * that write-back would look like "no change": nothing would be logged, and an offline file
     * would not be saved. The items would leave the player's file without leaving it on disk.
     */
    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot).copy();
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return delegate.isItemValid(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!writable.getAsBoolean()) return stack;
        if (simulate) return delegate.insertItem(slot, stack, true);
        ItemStack before = delegate.getStackInSlot(slot).copy();
        ItemStack left = delegate.insertItem(slot, stack, false);
        changed(slot, before);
        return left;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!writable.getAsBoolean()) return ItemStack.EMPTY;
        if (simulate) return delegate.extractItem(slot, amount, true);
        ItemStack before = delegate.getStackInSlot(slot).copy();
        ItemStack taken = delegate.extractItem(slot, amount, false);
        changed(slot, before);
        return taken;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (!writable.getAsBoolean()) return;
        ItemStack before = delegate.getStackInSlot(slot).copy();
        delegate.setStackInSlot(slot, stack);
        changed(slot, before);
    }

    private void changed(int slot, ItemStack before) {
        ItemStack now = delegate.getStackInSlot(slot);
        if (ItemStack.matches(before, now)) return;
        after.run();
        edits.changed(where.apply(slot), before, now.copy());
    }
}
