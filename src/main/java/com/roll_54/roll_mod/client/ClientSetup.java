package com.roll_54.roll_mod.client;

import com.roll_54.roll_mod.machine.client.LCRScreen;
import com.roll_54.roll_mod.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init() {
        MenuScreens.register(ModMenus.LARGE_CHEMICAL_REACTOR_MENU.get(), LCRScreen::new);
    }
}
