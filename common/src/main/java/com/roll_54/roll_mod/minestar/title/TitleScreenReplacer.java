package com.roll_54.roll_mod.minestar.title;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Swaps vanilla's title screen for {@link RollTitleScreen} every time one is about to open.
 *
 * <p>Every way back to the menu — the first screen after loading, leaving a world, a disconnect,
 * Back from a screen that was handed a fresh {@code new TitleScreen()} as its parent — goes through
 * {@code Minecraft.setScreen}, which posts {@link ScreenEvent.Opening} before the screen is added or
 * initialised, so the vanilla one is never shown. Replaces the pack's FancyMenu title layout.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class TitleScreenReplacer {

    private TitleScreenReplacer() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen) {
            event.setNewScreen(RollTitleScreen.create());
        }
    }
}
