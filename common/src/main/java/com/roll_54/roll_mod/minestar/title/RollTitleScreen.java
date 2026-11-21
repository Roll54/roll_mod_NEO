package com.roll_54.roll_mod.minestar.title;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.systems.RenderSystem;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.config.RollClientConfig;
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
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
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
 * <p>Everything is there at once, with no fade-in; only the players pop in a few seconds after the
 * menu first shows in a session. Opened by {@link TitleScreenReplacer}.
 */
@OnlyIn(Dist.CLIENT)
public final class RollTitleScreen extends ModularUIScreen {

    private static final String SUPPORT_URL = "https://minestar.com.ua/uk/packs/moderntech";
    private static final ResourceLocation LOGO = RollMod.id("textures/gui/title/moderntech_logo.png");
    private static final ResourceLocation ROLL_54_SKIN = RollMod.id("textures/gui/title/roll_54.png");
    private static final ResourceLocation ACCESSIBILITY_ICON = ResourceLocation.withDefaultNamespace("icon/accessibility");
    private static final Component COPYRIGHT = Component.translatable("title.credits");

    /** roll_54's walking pose, in degrees, from the layout. */
    private static final TitlePlayerElement.Pose WALKING =
            new TitlePlayerElement.Pose(-31.368078f, 0f, 25.504887f, 0.2931596f, 32.833878f, -0.5863192f, -32.24756f, -0.2931596f);

    private final List<Placement> placements = new ArrayList<>();
    private final List<TitlePlayerElement> players = new ArrayList<>();
    /** The optional Singleplayer button (Fzzy client config roll_mod:roll_mod_client). */
    private Button singleplayer;

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
        place(root, logo, -140, -128, 277, 45);

        // The mod's own splash lines now and then, vanilla's otherwise (RollSplashes).
        place(root, new TitleSplashElement(RollSplashes.pick(mc)), 57, -98, 115, 34);

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
        place(root, play, -100, -46, 200, 20);

        place(root, button(Component.translatable("gui.roll_mod.title.support"),
                () -> Util.getPlatform().openUri(SUPPORT_URL)), -100, -21, 200, 20);
        place(root, button(Component.translatable("fml.menu.mods"),
                () -> open(new ModListScreen(this))), -100, 4, 200, 20);
        place(root, button(Component.translatable("menu.options"),
                () -> open(new OptionsScreen(this, mc.options))), -100, 29, 85, 20);
        place(root, accessibilityButton(), -10, 29, 20, 20);
        place(root, button(Component.translatable("menu.quit"), mc::stop), 15, 29, 85, 19);

        // Optional small Singleplayer button under the accessibility one (Fzzy client config
        // roll_mod:roll_mod_client, titleScreen.singleplayerButton): the vanilla world list, for creating and playing local worlds.
        // Always built; shown or hidden on every showing (added()), so turning the option on in the
        // mod list's Config screen takes effect on coming back here, without a restart.
        singleplayer = button(Component.literal("+"), () -> open(new SelectWorldScreen(this)));
        singleplayer.style(s -> s.tooltips(Component.translatable("menu.singleplayer")));
        place(root, singleplayer, -10, 54, 20, 20);

        // Credits link, bottom-right as vanilla's.
        UIElement credits = new CreditsLink();
        credits.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).right(2).bottom(0)
                .width(mc.font.width(COPYRIGHT)).height(10));
        credits.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) open(new CreditsAndAttributionScreen(this));
        });
        root.addChild(credits);
    }

    /** Each time the menu is shown: the Singleplayer button follows its config option. */
    @Override
    public void added() {
        super.added();
        singleplayer.setDisplay(RollClientConfig.INSTANCE != null && RollClientConfig.INSTANCE.titleScreen.singleplayerButton);
    }

    /** Runs on opening and on every resize: positions everything for the current size, then lays out. */
    @Override
    public void init() {
        for (Placement placement : placements) placement.apply(width, height);
        super.init();
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

    /**
     * Vanilla's accessibility icon on a themed button. A plain {@link Button} with the icon as a child: a subclass of
     * Button is not the registered "button" element, so no theme's {@code button:host} rules reached it and it kept
     * LdLib's dark default whatever theme was picked.
     */
    private Button accessibilityButton() {
        Button button = new Button();
        UIElement icon = new UIElement();
        // Taffy's absolute, as place() uses: the icon takes no room in the button's own layout, so the button keeps
        // its full 20x20 (with the icon in the flow, the theme's 14px button height and padding cut its bottom off).
        icon.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(3).top(2).width(15).height(15));
        icon.style(s -> s.background(SpriteTexture.of(
                ResourceLocation.withDefaultNamespace("textures/gui/sprites/icon/accessibility.png"))));
        button.addChild(icon);
        button.layout(l -> l.paddingAll(0));
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
