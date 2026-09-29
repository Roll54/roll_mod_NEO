package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.annotation.Nullable;

/**
 * The way out of the hub: back to the player's own inventory, rather than back to the world.
 *
 * <p>The hub is reached <em>from</em> the inventory — {@code HubButtonHandler} puts the button there
 * — so the inventory is where leaving it should land you. Vanilla's own behaviour for a container
 * screen is to close to nothing at all, which drops the player into the world mid-thought and makes
 * them press {@code E} again to get back to where they started.
 *
 * <p>Two ways in, one behaviour: the {@code E} key (whatever the player has bound
 * {@code key.inventory} to) and the button in the hub's top-right corner — see {@code HubUI}.
 *
 * <p>Client-only. The hub's element tree is built on the dedicated server too, so {@code HubUI}
 * reaches {@link #toInventory()} through a dist check and an isolated holder, the same way
 * {@link HubInfo} and {@link HubSection} reach their client-side pieces.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class HubReturn {

    private HubReturn() {}

    /**
     * Closes the hub and opens the player's inventory in its place.
     *
     * <p>{@code closeContainer} is vanilla's own close, not a re-implementation of it: it sends the
     * {@code ServerboundContainerClosePacket}, puts both sides back on the player's inventory menu
     * and clears the screen. {@link InventoryScreen} then picks that menu up, so the player lands in
     * exactly the state pressing {@code E} in the world would have left them in — the hub's menu is
     * properly closed on the server, not merely hidden behind another screen.
     */
    public static void toInventory() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            minecraft.setScreen(null);
            return;
        }
        player.closeContainer();
        minecraft.setScreen(new InventoryScreen(player));
    }

    /**
     * Turns the inventory key into "back to the inventory" while the hub is open.
     *
     * <p>Intercepted here rather than inside the hub's element tree, because the tree never sees it:
     * {@code ModularUI} dispatches {@code keyDown} to its <em>focused element</em>, and with nothing
     * focused the key falls straight through to
     * {@code AbstractContainerScreen.keyPressed}, which closes to the world. There is no root
     * listener to hang this on.
     */
    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        ModularUI hub = hubOf(event.getScreen());
        if (hub == null) return;

        // A focused element gets first refusal, exactly as vanilla gives its focused widget first
        // refusal before checking the inventory key. Without this, typing an "e" into the warp or
        // home search box would slam the hub shut instead.
        if (hub.getFocusedElement() != null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) return;

        // Cancelled, or the screen would also run its own close and leave us setting a screen on a
        // screen that is already on its way out.
        event.setCanceled(true);
        toInventory();
    }

    /**
     * The screen class every LdLib container UI — the hub included — is shown in. Typed as a plain
     * container screen so callers without LdLib on their compile classpath (the client module) can
     * register against it.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Class<AbstractContainerScreen<?>> screenClass() {
        return (Class) ModularUIContainerScreen.class;
    }

    /**
     * The hub window's rectangle on {@code screen}, as {x, y, w, h} in GUI-scaled pixels, or
     * {@code null} when the screen is not the hub. For overlays such as JEI that need to know where
     * the GUI really is: the hub's own root now covers the whole screen, with the window floating on
     * it.
     */
    @Nullable
    public static float[] windowRect(@Nullable Screen screen) {
        ModularUI hub = hubOf(screen);
        return hub == null ? null : HubUI.windowRect(hub);
    }

    /**
     * The hub's {@link ModularUI} if that is what is on screen, otherwise {@code null} — every other
     * LdLib screen in the mod (the vendor, the buyer view) keeps vanilla's close-to-world behaviour.
     * Identified by the id {@code HubUI} stamps on its root element, which is cheaper and steadier
     * than trying to recognise the menu's holder.
     */
    @Nullable
    private static ModularUI hubOf(@Nullable Screen screen) {
        if (!(screen instanceof ModularUIContainerScreen container)) return null;
        ModularUI ui = container.getMenu().getModularUI();
        return HubUI.isHub(ui) ? ui : null;
    }
}
