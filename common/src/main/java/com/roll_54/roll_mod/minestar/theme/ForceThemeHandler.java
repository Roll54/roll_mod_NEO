package com.roll_54.roll_mod.minestar.theme;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.annotation.Nullable;

/**
 * Experimental: with "Use the hub theme everywhere" on in the hub's settings, every LdLib screen
 * that opens — other mods' ones included, such as MBD2 machines — is restyled in the player's theme.
 *
 * <p>Experimental because those screens were designed against their own stylesheets: some draw
 * fixed colours of their own on top, and may come out clashing or hard to read. The mod's own
 * screens already wear the theme (see {@link RollUi}); restyling them again changes nothing.
 * Runs on every init, so a resize keeps the theme too.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class ForceThemeHandler {

    private ForceThemeHandler() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!UiThemeState.forceEverywhere()) return;
        ModularUI ui = modularUI(event.getScreen());
        if (ui != null) RollUi.apply(ui);
    }

    @Nullable
    private static ModularUI modularUI(Screen screen) {
        if (screen instanceof ModularUIContainerScreen container) return container.getMenu().getModularUI();
        if (screen instanceof ModularUIScreen modular) return modular.getModularUI();
        return null;
    }
}
