package com.roll_54.roll_mod.minestar.welcome;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Keeps the inventory key from closing the starting screen. A container screen closes on it, and on
 * this screen a close is a refusal that kicks — too harsh for a reflexive {@code E}. Esc still
 * closes it, deliberately.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class WelcomeKeys {

    private WelcomeKeys() {}

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof ModularUIContainerScreen screen)) return;
        if (!WelcomeUI.isWelcome(screen.getMenu().getModularUI())) return;
        if (Minecraft.getInstance().options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
        }
    }
}
