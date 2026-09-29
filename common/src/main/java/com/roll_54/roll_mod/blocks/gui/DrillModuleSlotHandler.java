package com.roll_54.roll_mod.blocks.gui;

import com.roll_54.roll_mod.blocks.entity.ModuleInstallationTableBlockEntity;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleHelper;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleItem;
import com.roll_54.roll_mod.items.modulardrill.ModularTool;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * The module slots of the installation table's GUI, as a window into the drill's own component.
 *
 * <p>This handler holds no items. Every read reconstructs the module item from the id list in the
 * drill on the table, and every insert or extract rewrites that list — so the drill's component is
 * the single truth and the slots can never disagree with it, whatever happens to the drill stack
 * in between.
 *
 * <p>Modules fill left to right ({@code insert} only lands in the first free slot) and removing
 * one from the middle shifts the rest left. {@link #setStackInSlot} matters more than usual: a
 * shift-click or hotbar swap hands the player a copy from {@code getItem()} and then clears the
 * slot through {@code set(EMPTY)} — never touching {@code extractItem} — so {@code set(EMPTY)}
 * must really uninstall, or the copy in the player's hands is a duplicate. It is safe: the one
 * path that clears through {@code extractItem} first (plain pickup) only sends {@code set(EMPTY)}
 * when the slot is already empty, and an already-empty slot makes it a no-op.
 */
public final class DrillModuleSlotHandler implements IItemHandlerModifiable {

    /** Always the maximum any drill offers; slots past the current drill's count refuse items. */
    public static final int SLOTS = 7;

    private final ModuleInstallationTableBlockEntity table;

    public DrillModuleSlotHandler(ModuleInstallationTableBlockEntity table) {
        this.table = table;
    }

    private ItemStack drill() {
        return table.drill();
    }

    @Override
    public int getSlots() {
        return SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack drill = drill();
        if (!(drill.getItem() instanceof ModularTool)) return ItemStack.EMPTY;

        // Carries what the module stores (the Trash Filter's list), so the copy a shift-click or
        // swap hands out is the whole module.
        return DrillModuleHelper.moduleStack(drill, slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!mayInstallAt(slot, stack)) return stack;

        if (!simulate) {
            DrillModuleHelper.install(drill(), stack);
            table.drillChanged();
        }
        return stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        ItemStack current = getStackInSlot(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        // Refused for simulate too: SlotItemHandler.mayPickup asks exactly this, so pickup,
        // quick-move and swap are all stopped before anything is handed over.
        if (!DrillModuleHelper.canRemove(drill(), slot)) return ItemStack.EMPTY;
        if (simulate) return current;

        ItemStack removed = DrillModuleHelper.removeModule(drill(), slot);
        table.drillChanged();
        return removed;
    }

    /**
     * {@code EMPTY} uninstalls the module at this index — the menu's quick-move and swap paths
     * clear a slot this way after handing the player a copy from {@code getItem()}, so treating
     * it as a no-op duplicated the module (see the class comment). A module into an empty slot
     * installs; anything else leaves the component alone, and the next read shows the real state.
     */
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            // Guarded like extractItem; mayPickup already kept the module out of the player's hands.
            if (!getStackInSlot(slot).isEmpty() && DrillModuleHelper.canRemove(drill(), slot)) {
                DrillModuleHelper.removeModule(drill(), slot);
                table.drillChanged();
            }
            return;
        }
        if (!getStackInSlot(slot).isEmpty()) return;
        insertItem(slot, stack, false);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return mayInstallAt(slot, stack);
    }

    /** Valid only in the first free slot of a drill that accepts this module. */
    private boolean mayInstallAt(int slot, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof DrillModuleItem module)) return false;

        ItemStack drill = drill();
        if (!(drill.getItem() instanceof ModularTool tool)) return false;
        if (slot >= tool.moduleSlots()) return false;
        if (slot != DrillModuleHelper.get(drill).modules().size()) return false;

        return DrillModuleHelper.canInstall(drill, module);
    }
}
