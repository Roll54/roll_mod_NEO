package com.roll_54.roll_mod.minestar.moderation.invsee;

import net.minecraft.world.item.ItemStack;

/**
 * Told about every change a moderator makes through the inventory window, after it has happened:
 * which slot, in words for the audit log, and what was there before and after.
 */
@FunctionalInterface
public interface InvSeeEdit {

    /** For the client's placeholder handlers, which never edit anything real. */
    InvSeeEdit NONE = (where, before, after) -> {};

    void changed(String where, ItemStack before, ItemStack after);
}
