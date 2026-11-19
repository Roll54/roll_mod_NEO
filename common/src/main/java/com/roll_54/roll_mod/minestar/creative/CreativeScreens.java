package com.roll_54.roll_mod.minestar.creative;

import net.minecraft.world.item.ItemStack;

/**
 * {@link RollCreativeScreen} for code that does not compile against LdLib (the client module's TEI mixins): every
 * check takes a plain {@code Object} screen, like {@code HubScreens}. Client only.
 */
public final class CreativeScreens {

    private CreativeScreens() {}

    /** Whether {@code screen} is roll_mod's creative inventory. */
    public static boolean isCreative(Object screen) {
        return screen instanceof RollCreativeScreen;
    }

    /** Whether {@code screen} is the creative inventory with its search bar focused. */
    public static boolean isTyping(Object screen) {
        return screen instanceof RollCreativeScreen creative && creative.isTyping();
    }

    /** The item under the mouse in the creative inventory; empty for any other screen or when there is none. */
    public static ItemStack hovered(Object screen) {
        return screen instanceof RollCreativeScreen creative ? creative.hoveredStack() : ItemStack.EMPTY;
    }
}
