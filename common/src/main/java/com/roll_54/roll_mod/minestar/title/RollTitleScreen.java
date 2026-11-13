package com.roll_54.roll_mod.minestar.title;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.systems.RenderSystem;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.internal.BrandingControl;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The pack's title screen, built in LdLib and hardcoded from what used to be a FancyMenu layout
 * ({@code config/fancymenu/customization/title_screen_layout.txt} in the client pack): the ModernTech
 * logo, vanilla's splash line, two players either side, and a short button column — Play (the server
 * list), a support link, Mods, then Options, Accessibility and Quit on one row. Singleplayer, Realms,
 * the language button and Minecraft's own logo are gone; the panorama, the branding lines and the
 * credits link stay where vanilla has them. Wears the hub theme ({@link RollUi}).
 *
 * <p>Every position is an offset of the element's top-left corner from the screen's centre, in GUI
 * pixels, copied from the layout, so the column stays centred at any window size or GUI scale. On a
 * screen too small for an offset the element is pushed back inside the edge, as the layout's
 * "stay on screen" did.
 *
 * <p>Elements come in one after another, as in the layout. Logo, splash and players do that only the
 * first time the menu is shown in a session; the buttons fade in every time the menu comes back.
 * Opened by {@link TitleScreenReplacer}.
 */
@OnlyIn(Dist.CLIENT)
public final class RollTitleScreen extends ModularUIScreen {

    private static final String SUPPORT_URL = "https://minestar.com.ua/uk/packs/moderntech";
    private static final ResourceLocation LOGO = RollMod.id("textures/gui/title/moderntech_logo.png");
    private static final ResourceLocation ROLL_54_SKIN = RollMod.id("textures/gui/title/roll_54.png");
    private static final ResourceLocation ACCESSIBILITY_ICON = ResourceLocation.withDefaultNamespace("icon/accessibility");
    private static final Component COPYRIGHT = Component.translatable("title.credits");

    /** Seconds an element takes to fade in. */
    private static final float FADE = 0.5f;
    /** The players' delay on first open: they pop in, no fade. */
    private static final long PLAYERS_DELAY_MS = 3000;
    /** roll_54's walking pose, in degrees, from the layout. */
    private static final TitlePlayerElement.Pose WALKING =
            new TitlePlayerElement.Pose(-31.368078f, 0f, 25.504887f, 0.2931596f, 32.833878f, -0.5863192f, -32.24756f, -0.2931596f);

    /** Whether the first-open-only entrances have been played this session. */
    private static boolean introPlayed;

    /** An element that fades in {@code delay} seconds after the menu shows; {@code everyTime}: on every showing, not just the first. */
    private record Entrance(UIElement element, float delay, boolean everyTime) {}

    private final List<Placement> placements = new ArrayList<>();
    private final List<Entrance> entrances = new ArrayList<>();
    private final List<TitlePlayerElement> players = new ArrayList<>();
    private final List<ISubscription> running = new ArrayList<>();

    private RollTitleScreen(UIElement root) {
        super(ModularUI.of(UI.of(root, RollUi.sheets(), size -> size)).shouldCloseOnEsc(false),
                Component.translatable("narrator.screen.title"));
    }

    public static RollTitleScreen create() {
        UIElement root = new UIElement();
        root.layout(l -> l.widthPercent(100).heightPercent(100));
        RollTitleScreen screen = new RollTitleScreen(root);
        screen.build(root);
        return screen;
    }

    private void build(UIElement root) {
        Minecraft mc = Minecraft.getInstance();

        UIElement logo = new UIElement();
        logo.style(s -> s.backgroundTexture(SpriteTexture.of(LOGO)));
        entrances.add(new Entrance(place(root, logo, -140, -128, 277, 45), 1.0f, false));

        entrances.add(new Entrance(place(root, new TitleSplashElement(mc.getSplashManager().getSplash()), 57, -98, 115, 34), 1.0f, false));

        PlayerSkin roll54 = new PlayerSkin(ROLL_54_SKIN, null, null, null, PlayerSkin.Model.SLIM, false);
        players.add(place(root, new TitlePlayerElement(() -> roll54, "roll_54", WALKING), -158, -30, 18, 54));
        Supplier<PlayerSkin> ownSkin = mc.getSkinManager().lookupInsecure(mc.getGameProfile());
        players.add(place(root, new TitlePlayerElement(ownSkin, mc.getUser().getName(), TitlePlayerElement.Pose.STANDING), 145, -30, 18, 54));

        Component multiplayerDisabled = multiplayerDisabledReason(mc);
        Button play = button(Component.translatable("gui.roll_mod.title.play"), () -> open(
                mc.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)));
        if (multiplayerDisabled != null) {
            play.setActive(false);
            play.style(s -> s.tooltips(multiplayerDisabled));
        }
        entrances.add(new Entrance(place(root, play, -100, -46, 200, 20), 0.5f, true));

        entrances.add(new Entrance(place(root, button(Component.translatable("gui.roll_mod.title.support"),
                () -> Util.getPlatform().openUri(SUPPORT_URL)), -100, -21, 200, 20), 0.7f, true));
        entrances.add(new Entrance(place(root, button(Component.translatable("fml.menu.mods"),
                () -> open(new ModListScreen(this))), -100, 4, 200, 20), 0.9f, true));
        entrances.add(new Entrance(place(root, button(Component.translatable("menu.options"),
                () -> open(new OptionsScreen(this, mc.options))), -100, 29, 85, 20), 1.1f, true));
        entrances.add(new Entrance(place(root, accessibilityButton(), -10, 29, 20, 20), 1.1f, true));
        entrances.add(new Entrance(place(root, button(Component.translatable("menu.quit"), mc::stop), 15, 29, 85, 19), 1.1f, true));

        // Credits link, bottom-right as vanilla's.
        UIElement credits = new CreditsLink();
        credits.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).right(2).bottom(0)
                .width(mc.font.width(COPYRIGHT)).height(10));
        credits.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) open(new CreditsAndAttributionScreen(this));
        });
        root.addChild(credits);
    }

    /* ------------------------------------------------------ showing ------------------------------------------------------ */

    /** Each time the menu is shown — opened, or come back to from a screen it opened — the entrances replay. */
    @Override
    public void added() {
        super.added();
        boolean first = !introPlayed;
        introPlayed = true;

        running.forEach(ISubscription::unsubscribe);
        running.clear();
        for (Entrance entrance : entrances) {
            boolean animate = entrance.everyTime() || first;
            if (animate) entrance.element().style(s -> s.opacity(0f));
            // Already shown before: straight to fully visible — still as an animation, so one cut short
            // by leaving the menu early cannot leave its half-faded value behind.
            float delay = animate ? entrance.delay() : 0f;
            float duration = animate ? FADE : 0.001f;
            entrance.element().animation(a -> running.add(a.delay(delay).duration(duration)
                    .style(PropertyRegistry.OPACITY, 1f).start()));
        }
        for (TitlePlayerElement player : players) player.hideFor(first ? PLAYERS_DELAY_MS : 0);
    }

    /** Runs on opening and on every resize: positions everything for the current size, then lays out. */
    @Override
    public void init() {
        for (Placement placement : placements) placement.apply(width, height);
        super.init();
    }

    @Override
    public void removed() {
        running.forEach(ISubscription::unsubscribe);
        running.clear();
        super.removed();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /* ----------------------------------------------------- rendering ----------------------------------------------------- */

    /** Vanilla's title panorama, unblurred. {@link ModularUIScreen}'s own background draws nothing. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanorama(graphics, partialTick);
    }

    /** The UI, then what vanilla's {@code TitleScreen.render} draws around its buttons. */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int white = 0xFFFFFFFF;
        // NeoForge's snapshot warning, and the update line the branding below picks up. Its screen
        // parameter is unused — this screen is not a TitleScreen.
        ClientHooks.renderMainMenu(null, graphics, font, width, height, white);
        BrandingControl.forEachLine(true, true, (line, text) ->
                graphics.drawString(font, text, 2, height - (10 + line * (font.lineHeight + 1)), white));
        BrandingControl.forEachAboveCopyrightLine((line, text) ->
                graphics.drawString(font, text, width - font.width(text), height - (10 + (line + 1) * (font.lineHeight + 1)), white));
    }

    /* ------------------------------------------------------ helpers ------------------------------------------------------ */

    /**
     * Adds {@code element}, sized {@code w × h}, with its top-left corner at {@code (x, y)} from the
     * screen's centre. The position itself is set on every {@link #init}, once the screen size is known.
     */
    private <T extends UIElement> T place(UIElement root, T element, float x, float y, float w, float h) {
        element.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).width(w).height(h));
        placements.add(new Placement(element, x, y, w, h));
        root.addChild(element);
        return element;
    }

    /** An element's offset from the screen's centre, and its size. */
    private record Placement(UIElement element, float x, float y, float w, float h) {
        /** At its offset, but pushed back inside a screen too small for it — the layout's "stay on screen". */
        void apply(int screenWidth, int screenHeight) {
            float left = Math.max(0, Math.min(screenWidth / 2f + x, screenWidth - w));
            float top = Math.max(0, Math.min(screenHeight / 2f + y, screenHeight - h));
            element.layout(l -> l.left(left).top(top));
        }
    }

    private static Button button(Component text, Runnable onClick) {
        Button button = new Button();
        button.setText(text);
        button.setOnClick(e -> onClick.run());
        return button;
    }

    /** Vanilla's accessibility icon on a themed button. */
    private Button accessibilityButton() {
        Button button = new Button() {
            @Override
            public void drawBackgroundAdditional(GUIContext context) {
                super.drawBackgroundAdditional(context);
                context.graphics.flush();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.graphics.blitSprite(ACCESSIBILITY_ICON,
                        Math.round(getPositionX() + (getSizeWidth() - 15) / 2f),
                        Math.round(getPositionY() + (getSizeHeight() - 15) / 2f), 15, 15);
                context.graphics.flush();
            }
        };
        button.noText();
        button.setOnClick(e -> open(new AccessibilityOptionsScreen(this, Minecraft.getInstance().options)));
        button.style(s -> s.tooltips(Component.translatable("options.accessibility")));
        return button;
    }

    /**
     * Opens {@code screen} on the next frame rather than from inside LdLib's click handling, so this
     * UI is not torn down while it is still dispatching the click.
     */
    private static void open(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> mc.setScreen(screen));
    }

    /** Why multiplayer is greyed out, or null when it isn't — vanilla's {@code TitleScreen.getMultiplayerDisabledReason}. */
    @Nullable
    private static Component multiplayerDisabledReason(Minecraft mc) {
        if (mc.allowsMultiplayer()) return null;
        if (mc.isNameBanned()) return Component.translatable("title.multiplayer.disabled.banned.name");
        BanDetails ban = mc.multiplayerBan();
        if (ban != null) {
            return ban.expires() != null
                    ? Component.translatable("title.multiplayer.disabled.banned.temporary")
                    : Component.translatable("title.multiplayer.disabled.banned.permanent");
        }
        return Component.translatable("title.multiplayer.disabled");
    }

    /** Vanilla's credits link: plain white text, underlined while hovered. */
    private static final class CreditsLink extends UIElement {
        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);
            Component text = isHover() ? COPYRIGHT.copy().withStyle(s -> s.withUnderlined(true)) : COPYRIGHT;
            context.graphics.drawString(Minecraft.getInstance().font, text,
                    Math.round(getPositionX()), Math.round(getPositionY()), 0xFFFFFFFF);
        }
    }
}
