package com.roll_54.roll_mod.minestar.moderation.invsee;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A slot of the inventory window. A read-only window locks it on both sides: the client does not
 * even try a move the server will refuse, so nothing flickers. {@code max} is the most one slot
 * holds (1 for armour and implants). The client's placeholder handlers do not know that, so the
 * slot does.
 */
final class InvSeeSlot extends SlotItemHandler {

    private final boolean editable;
    private final int max;

    InvSeeSlot(IItemHandler handler, int index, boolean editable, int max) {
        super(handler, index, 0, 0);
        this.editable = editable;
        this.max = max;
    }

    @Override
    public boolean mayPickup(Player player) {
        return editable && super.mayPickup(player);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return editable && super.mayPlace(stack);
    }

    @Override
    public int getMaxStackSize() {
        return Math.min(max, super.getMaxStackSize());
    }

    /**
     * Not {@code super}: {@code SlotItemHandler} measures this by setting the slot to EMPTY,
     * simulating an insert, and setting it back. Here that write is real. An implant would be
     * uninstalled and reinstalled with every hook, and an offline file saved twice, on every click.
     */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(max, Math.min(stack.getMaxStackSize(), getItemHandler().getSlotLimit(getSlotIndex())));
    }
}
