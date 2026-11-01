package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.kits.KitWindows;
import com.roll_54.roll_mod.network.packet.OpenHubPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The kit window's way back to the hub ({@code KitWindowUI}'s Back button): the editor returns to
 * the moderation tab, in Kits mode on the same kit ({@link KitNamesPanel#takeReturnTo}); the preview
 * to the home tab, where the kits are listed. Esc closes the window as any other, back to the game.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class KitWindowReturn {

    /** Set while Back is closing the window, so that close keeps the kit to come back to. */
    private static boolean goingBack;

    private KitWindowReturn() {}

    public static void back(boolean editor) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        goingBack = true;
        // The close reaches the server first (the editor saves on it); then the hub opens.
        player.closeContainer();
        PacketDistributor.sendToServer(new OpenHubPacket(HubUI.indexOf(editor ? "moderation" : "home")));
    }

    /** Closed any other way: nothing to come back to, so the next hub opens as it normally would. */
    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing event) {
        if (!(event.getScreen() instanceof ModularUIContainerScreen screen)
                || !(screen.getMenu() instanceof ModularUIContainerMenu menu)
                || !(menu.uiHolder instanceof KitWindows.Holder)) {
            return;
        }
        if (!goingBack) KitNamesPanel.takeReturnTo();
        goingBack = false;
    }
}
