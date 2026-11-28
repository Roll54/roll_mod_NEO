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
 * Takes other mods' buttons back off roll_mod's own full screens: FTB Library's sidebar off the hub, the starting
 * screen and the ore scanner; Sophisticated Inventory Interactions' sort buttons off the ore scanner and the Crop
 * Manager.
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

    /**
     * Sophisticated Inventory Interactions' sort and transfer buttons, which it adds beside the player's inventory on
     * every container screen; on the Crop Manager one sat over the end of the energy bar.
     */
    private static final String SOPHISTICATED = "net.p3pp3rf1y.sophisticated";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ModularUIContainerScreen screen)) return;
        ModularUI ui = screen.getMenu().getModularUI();
        boolean scanner = com.roll_54.roll_mod.items.electricItems.orescanner.gui.OreScannerUI.isOreScanner(ui);
        boolean cropManager = com.roll_54.roll_mod.compat.MBD2.machine.CropManagerUI.isCropManager(ui);
        // FTB's sidebar (teams, quests, time and weather) off the hub, the starting screen and the ore scanner.
        boolean dropSidebar = HubUI.isHub(ui) || WelcomeUI.isWelcome(ui) || scanner;
        // Other mods' inventory buttons off the screens that have no player-inventory use for them.
        boolean dropSophisticated = scanner || cropManager;
        if (!dropSidebar && !dropSophisticated) return;

        List<GuiEventListener> foreign = new ArrayList<>();
        for (GuiEventListener listener : event.getListenersList()) {
            String name = listener.getClass().getName();
            if ((dropSidebar && FTB_SIDEBAR.equals(name)) || (dropSophisticated && name.startsWith(SOPHISTICATED))) {
                foreign.add(listener);
            }
        }
        foreign.forEach(event::removeListener);
    }
}
