package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.gui.HubBadge;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import com.roll_54.roll_mod.minestar.letters.ClientLetterCache;
import com.roll_54.roll_mod.minestar.moderation.ClientModerationCache;
import com.roll_54.roll_mod.minestar.moderation.ClientPlayerStatusCache;
import com.roll_54.roll_mod.network.packet.OpenHubPacket;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Adds a hub button to the inventory, directly under the recipe-book button.
 *
 * <p>Done with NeoForge's {@link ScreenEvent} rather than a mixin, unlike the rest of this package.
 * Vanilla keeps its recipe button in a local variable and repositions it from inside a synthetic
 * lambda, so there is no stable member to inject against — and this mixin config sets
 * {@code injectors.defaultRequire = 1}, which turns a missed injection into a crash. The screen
 * event needs no injection point and {@code getGuiLeft()} is public, so nothing has to be shadowed.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class HubButtonHandler {

    /** The whole button, frame included — {@code WIDTH x HEIGHT}, not an icon laid over a plate. */
    private static final ResourceLocation ICON = RollMod.id("textures/gui/hub/main.png");
    /** The same button, hovered or focused. */
    private static final ResourceLocation ICON_HOVERED = RollMod.id("textures/gui/hub/main_overlay.png");

    /** Matches vanilla's recipe button, which is 20x18 at {@code leftPos + 104, topPos + 61}. */
    private static final int WIDTH = 20;
    private static final int HEIGHT = 18;
    private static final int OFFSET_X = 104 + WIDTH + 1;   // immediately right of it
    private static final int OFFSET_Y = 61;                // and on the same row

    /** The fallback dot's size, while {@code exclamation_mark.png} is not in the pack. */
    private static final int BADGE_SIZE = 5;

    private HubButtonHandler() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        HubButton button = new HubButton(
                screen.getGuiLeft() + OFFSET_X, screen.getGuiTop() + OFFSET_Y);
        event.addListener(button);
    }

    /**
     * Keeps the button under the recipe button as that button moves.
     *
     * <p>Toggling the recipe book shifts {@code leftPos} by nearly ninety pixels, and vanilla
     * repositions its own button inside the toggle handler — anything placed once at init is left
     * behind. Following it every frame is cheap and cannot get out of step.
     */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        for (var child : screen.children()) {
            if (child instanceof HubButton button) {
                button.setPosition(screen.getGuiLeft() + OFFSET_X, screen.getGuiTop() + OFFSET_Y);
                button.refreshTooltip();
            }
        }
    }

    /** Standing belongs to the server that sent it; the next server sends its own. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPlayerStatusCache.clear();
        ClientModerationCache.clear();
        ClientLetterCache.accept(java.util.List.of());
        // The hub's own caches are only refilled when the hub is next opened, so a badge from the
        // last server would otherwise sit on the button until then.
        com.roll_54.roll_mod.minestar.tpa.ClientTpaCache.INCOMING = java.util.List.of();
        com.roll_54.roll_mod.minestar.hub.home.ClientHomeCache.HOMES = java.util.List.of();
        com.roll_54.roll_mod.economy.vendingblock.client.ClientAuctionCache.set(
                new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    /**
     * The button: {@code hub/main.png}, swapped for {@code hub/main_overlay.png} while hovered.
     *
     * <p>Both are plain PNGs rather than atlas sprites — this repo has no
     * {@code textures/gui/sprites/} convention, and blitting a file is what it does everywhere else,
     * see {@code MoneyShovingMixin}. Each carries its own frame, so neither vanilla's
     * {@code widget/button} nor the hub plate is drawn under it.
     */
    private static final class HubButton extends Button {

        private Component shownTip;

        private HubButton(int x, int y) {
            // Empty message: the icon is the label, and vanilla would otherwise draw text over it.
            // The name lives in the tooltip instead, where it also serves the narrator.
            // Opens on the remembered tab, same as the hub key: the button and the key are two ways
            // to reach one screen, so landing somewhere different depending on which was used only
            // costs the player the click back. See HubUI.lastTab.
            super(x, y, WIDTH, HEIGHT, Component.empty(),
                    b -> PacketDistributor.sendToServer(new OpenHubPacket(HubUI.lastTab())),
                    DEFAULT_NARRATION);
            refreshTooltip();
        }

        /**
         * The name, plus a line for everything the badge stands for — so hovering the dot says
         * why it is there. Rebuilt every frame because a mute counts down while the screen is open.
         */
        private static Component line(String key) {
            return Component.translatable(key).withStyle(net.minecraft.ChatFormatting.AQUA);
        }

        private void refreshTooltip() {
            MutableComponent tip = Component.translatable("gui.roll_mod.hub.button.tip");
            for (Component line : ClientPlayerStatusCache.alertLines()) {
                tip.append("\n").append(line);
            }
            if (HubBadge.tpaPending()) tip.append("\n").append(line("gui.roll_mod.hub.status.tpa"));
            if (HubBadge.homeInvitePending()) tip.append("\n").append(line("gui.roll_mod.hub.status.homeInvite"));
            if (HubBadge.auctionPending()) tip.append("\n").append(line("gui.roll_mod.hub.status.auction"));
            if (HubBadge.moderationPending()) tip.append("\n").append(line("gui.roll_mod.hub.status.warpsPending"));
            // Only when it changed: Tooltip caches its wrapped lines, and a fresh one per frame
            // would throw that away sixty times a second for nothing.
            if (tip.equals(shownTip)) return;
            shownTip = tip;
            setTooltip(Tooltip.create(tip));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // No super: that blits vanilla's widget/button, which the textures replace outright.
            // Keyboard focus gets the hover art too: both mean the button is what a press would hit.
            ResourceLocation texture = isHoveredOrFocused() ? ICON_HOVERED : ICON;
            graphics.blit(texture, getX(), getY(), 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);

            // The same mark the hub's bookmarks wear — see HubBadge — at the same corner.
            if (HubBadge.hubButton()) {
                if (HubBadge.hasTexture()) {
                    int x = getX() + WIDTH - HubBadge.SIZE;
                    graphics.blit(HubBadge.TEXTURE, x, getY(), 0, 0,
                            HubBadge.SIZE, HubBadge.SIZE, HubBadge.SIZE, HubBadge.SIZE);
                } else {
                    int x = getX() + WIDTH - BADGE_SIZE - 1;
                    int y = getY() + 1;
                    graphics.fill(x - 1, y - 1, x + BADGE_SIZE + 1, y + BADGE_SIZE + 1, HubBadge.DOT_RIM);
                    graphics.fill(x, y, x + BADGE_SIZE, y + BADGE_SIZE, HubBadge.DOT_FILL);
                }
            }
        }
    }
}
