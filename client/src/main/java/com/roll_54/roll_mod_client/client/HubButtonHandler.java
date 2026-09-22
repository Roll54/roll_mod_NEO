package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.gui.HubPlate;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import com.roll_54.roll_mod.network.packet.OpenHubPacket;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

    private static final ResourceLocation ICON = RollMod.id("textures/gui/hub/main.png");

    /** Matches vanilla's recipe button, which is 20x18 at {@code leftPos + 104, topPos + 61}. */
    private static final int WIDTH = 20;
    private static final int HEIGHT = 18;
    private static final int OFFSET_X = 104 + WIDTH + 1;   // immediately right of it
    private static final int OFFSET_Y = 61;                // and on the same row

    private static final int ICON_SIZE = 16;

    /**
     * The cue for "the cursor is on this".
     *
     * <p>An outline rather than a wash: the plate is already near-white, so a white tint over it
     * would be invisible, and darkening it would read as pressed rather than as hovered.
     */
    private static final int HOVER_OUTLINE = 0xFFFFFFFF;

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
            }
        }
    }

    /**
     * The button: the hub's own bright plate, with {@code hub/main.png} blitted over it.
     *
     * <p>The icon is a plain PNG rather than an atlas sprite — this repo has no
     * {@code textures/gui/sprites/} convention, and blitting a file is what it does everywhere else,
     * see {@code MoneyShovingMixin}. The plate is not vanilla's {@code widget/button}, which is the
     * dark grey one and sat oddly beside the bright recipe-book button next to it.
     */
    private static final class HubButton extends Button {

        private HubButton(int x, int y) {
            // Empty message: the icon is the label, and vanilla would otherwise draw text over it.
            // The name lives in the tooltip instead, where it also serves the narrator.
            // Opens on the remembered tab, same as the hub key: the button and the key are two ways
            // to reach one screen, so landing somewhere different depending on which was used only
            // costs the player the click back. See HubUI.lastTab.
            super(x, y, WIDTH, HEIGHT, Component.empty(),
                    b -> PacketDistributor.sendToServer(new OpenHubPacket(HubUI.lastTab())),
                    DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.translatable("gui.roll_mod.hub.button.tip")));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // No super: that blits vanilla's widget/button, the dark grey plate this replaces.
            HubPlate.draw(graphics, getX(), getY(), WIDTH, HEIGHT);

            // Vanilla's chrome carried the hover cue; a plate that never changes needs its own.
            // Keyboard focus gets the same mark: both mean the button is what a press would hit.
            if (isHoveredOrFocused()) {
                graphics.renderOutline(getX(), getY(), WIDTH, HEIGHT, HOVER_OUTLINE);
            }

            graphics.blit(ICON, getX() + (WIDTH - ICON_SIZE) / 2, getY() + (HEIGHT - ICON_SIZE) / 2,
                    0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }
}
