package com.roll_54.roll_mod.minestar.hub.gui;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.network.packet.OpenHubPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The "Back to kits" button over a kit's items chest ({@code KitEditorMenu}, recognised by its
 * title). It closes the chest — which saves the kit — and opens the hub again on the moderation tab,
 * in Kits mode on the same kit. Esc and the inventory key close the chest as any chest does, back to
 * the game.
 *
 * @deprecated The chest editor is replaced by the LdLib kit window, whose own Back button is
 *             {@link KitWindowReturn}. Kept, inert, while {@code KitEditorMenu} is.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
@Deprecated
public final class KitEditorBackButton {

    private static final String TITLE_KEY = "gui.roll_mod.kit.editor.title";
    private static final int W = 100;
    private static final int H = 18;

    /** Set while the button is closing the chest, so that close keeps the kit to come back to. */
    private static boolean goingBack;

    private KitEditorBackButton() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !isKitEditor(screen)) return;
        int x = screen.getGuiLeft();
        int y = Math.max(2, screen.getGuiTop() - H - 2);
        event.addListener(Button.builder(Component.translatable("gui.roll_mod.kit.editor.back"), b -> back())
                .bounds(x, y, W, H).build());
    }

    private static void back() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        goingBack = true;
        // The close reaches the server first and saves the kit; then the hub opens on top of it.
        player.closeContainer();
        PacketDistributor.sendToServer(new OpenHubPacket(HubUI.indexOf("moderation")));
    }

    /** Closed any other way: nothing to come back to, so the next hub opens as it normally would. */
    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !isKitEditor(screen)) return;
        if (!goingBack) KitNamesPanel.takeReturnTo();
        goingBack = false;
    }

    private static boolean isKitEditor(AbstractContainerScreen<?> screen) {
        return screen.getTitle().getContents() instanceof TranslatableContents title && TITLE_KEY.equals(title.getKey());
    }
}
