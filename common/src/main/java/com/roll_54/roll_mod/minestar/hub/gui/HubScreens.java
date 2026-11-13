package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;

/**
 * Whether a screen is the hub, for code that cannot see LdLib (the client module's recipe viewer mixins). Client
 * only: only ever called with a screen in hand.
 */
public final class HubScreens {

    private HubScreens() {}

    public static boolean isHub(Object screen) {
        return screen instanceof ModularUIContainerScreen container && HubUI.isHub(container.getMenu().getModularUI());
    }
}
