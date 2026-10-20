package com.roll_54.roll_mod.minestar.hub.gui;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Which edge of the hub window the tab bookmarks sit along — set in the hub's settings. */
public enum HubTabSide {
    TOP, BOTTOM, LEFT, RIGHT;

    /** Bookmarks in a column rather than a row. */
    public boolean vertical() {
        return this == LEFT || this == RIGHT;
    }

    public Component title() {
        return Component.translatable("gui.roll_mod.hub.settings.tabs." + name().toLowerCase(Locale.ROOT));
    }

    static HubTabSide byName(String name) {
        for (HubTabSide side : values()) {
            if (side.name().equalsIgnoreCase(name)) return side;
        }
        return TOP;
    }
}
