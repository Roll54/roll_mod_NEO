package com.roll_54.roll_mod.hydroponics;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A stable handle on a bed's three shared input slots.
 *
 * <p>The slots belong to the node, and the node object is replaced whenever beds merge or a field
 * splits. Anything holding the handler directly — an open menu's slots, a cached capability — would
 * keep writing into the discarded one and quietly eat the player's items. Resolving on every call
 * costs a map lookup and removes the whole class of bug.
 *
 * <p>Modifiable, and not by choice: vanilla writes a synced stack back with {@code Slot#set}, and
 * NeoForge's {@link net.neoforged.neoforge.items.SlotItemHandler#set} casts its handler to {@link
 * IItemHandlerModifiable} to do it. Since {@code AbstractContainerMenu#initializeContents} calls
 * that for every slot on every container open, a view that implemented only {@link IItemHandler}
 * threw {@code ClassCastException} the moment the bed's GUI appeared.
 */
public record NodeItemHandlerView(Supplier<IItemHandler> target) implements IItemHandlerModifiable {

    private IItemHandler delegate() {
        IItemHandler handler = target.get();
        return handler == null ? EmptyHandler.INSTANCE : handler;
    }

    @Override
    public int getSlots() {
        return delegate().getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return delegate().getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return delegate().insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return delegate().extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate().getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return delegate().isItemValid(slot, stack);
    }

    /**
     * Only the client's own stand-in handler and the node's {@code ItemStackHandler} are modifiable;
     * a delegate that is not simply drops the write, which is what the empty stand-in wants anyway.
     */
    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (delegate() instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack);
        }
    }

    /** Stand-in for the window between a bed being placed and its node existing. */
    private enum EmptyHandler implements IItemHandlerModifiable {
        INSTANCE;

        @Override
        public int getSlots() {
            return 3;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            // Nothing to hold it in: this handler exists only so callers have something to talk to.
        }
    }
}
