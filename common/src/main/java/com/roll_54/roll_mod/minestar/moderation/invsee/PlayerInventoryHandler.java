package com.roll_54.roll_mod.minestar.moderation.invsee;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * A player's vanilla inventory as a handler: 0–35 main, 36–39 armour (feet to head), 40 offhand.
 *
 * <p>A handler rather than plain {@code Slot}s on the {@link Inventory}: LdLib2 counts any slot
 * whose container is an {@code Inventory} as the viewer's own side for shift-click. Once it is
 * someone else's inventory, that would send shift-clicks the wrong way.
 *
 * <p>The armour slots take what vanilla's armour slots take, one at a time.
 */
final class PlayerInventoryHandler extends InvWrapper {

    private static final EquipmentSlot[] ARMOUR = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private static final int ARMOUR_START = 36;

    private final Inventory inventory;

    PlayerInventoryHandler(Inventory inventory) {
        super(inventory);
        this.inventory = inventory;
    }

    /** The armour piece a slot holds, or {@code null} for every other slot. */
    static EquipmentSlot armour(int slot) {
        int i = slot - ARMOUR_START;
        return i >= 0 && i < ARMOUR.length ? ARMOUR[i] : null;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        EquipmentSlot armour = armour(slot);
        if (armour != null) return stack.canEquip(armour, inventory.player);
        return super.isItemValid(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!stack.isEmpty() && !isItemValid(slot, stack)) return stack;
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return armour(slot) != null ? 1 : super.getSlotLimit(slot);
    }
}
