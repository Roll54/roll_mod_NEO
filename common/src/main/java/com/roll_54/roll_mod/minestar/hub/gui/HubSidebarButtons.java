package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.welcome.WelcomeUI;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes FTB Library's sidebar buttons back off the hub and the starting screen.
 *
 * <p>FTB Library adds its button group to <em>every</em> {@code AbstractContainerScreen} on screen
 * init ({@code FTBLibraryClient.guiInit}), and an LdLib screen is one. Over the hub they sit on top
 * of a full-screen UI that has its own way to everything; over the starting screen they are worse —
 * opening anything from there closes the screen, which kicks.
 *
 * <p>Removed after the fact, at {@link EventPriority#LOWEST} so FTB's own handler has already run,
 * and matched by class name so FTB Library stays a soft dependency. It runs again on every init,
 * resizes included, which is exactly when FTB re-adds them.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class HubSidebarButtons {

    private static final String FTB_SIDEBAR = "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton";

    private HubSidebarButtons() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ModularUIContainerScreen screen)) return;
        ModularUI ui = screen.getMenu().getModularUI();
        if (!HubUI.isHub(ui) && !WelcomeUI.isWelcome(ui)) return;

        List<GuiEventListener> sidebar = new ArrayList<>();
        for (GuiEventListener listener : event.getListenersList()) {
            if (FTB_SIDEBAR.equals(listener.getClass().getName())) sidebar.add(listener);
        }
        sidebar.forEach(event::removeListener);
    }
}
