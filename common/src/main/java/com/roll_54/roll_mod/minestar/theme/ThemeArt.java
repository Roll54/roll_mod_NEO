package com.roll_54.roll_mod.minestar.theme;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * The hub theme's own art for vanilla's widgets: the button (normal, hovered, pressed, disabled), the text field and
 * the panel, taken from LdLib elements styled by the current theme's sheets — so a vanilla button, field or menu
 * background drawn with these looks exactly like the hub's, not like vanilla grey tinted by {@link GuiRecolor}.
 *
 * <p>Built once per theme (and corner setting) and kept; rebuilt when either changes. Only used while "Use the hub
 * theme everywhere" is on and a vanilla (not LdLib) screen is up — see {@link #active}.
 */
@OnlyIn(Dist.CLIENT)
public final class ThemeArt {

    public record Art(IGuiTexture button, IGuiTexture buttonHover, IGuiTexture buttonDisabled, IGuiTexture field,
                      IGuiTexture panel) {}

    @Nullable private static Art cached;
    @Nullable private static UiTheme cachedTheme;
    private static boolean cachedSquare;
    private static boolean failed;

    private ThemeArt() {}

    /**
     * The art to draw vanilla widgets with right now, or null when they should stay as they are: the setting is off,
     * no screen is up (the HUD), or the screen is LdLib's own.
     */
    @Nullable
    public static Art active() {
        // The vanilla-looking themes (Minecraft, plain OreUI) leave vanilla's widgets alone too.
        if (failed || !UiThemeState.forceEverywhere() || UiThemeState.get().recolor() == null) return null;
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null || isAe2(screen)) return null;
        if (screen instanceof com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen
                || screen instanceof com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen) return null;
        return get();
    }

    /**
     * The art for vanilla widgets on the screen up now: {@link #active()}, or on a screen the container skin themes
     * (the anvil…), the theme's art even with "Use the hub theme everywhere" off.
     */
    @Nullable
    public static Art forScreen() {
        Art art = active();
        if (art != null) return art;
        Screen screen = Minecraft.getInstance().screen;
        return screen != null && com.roll_54.roll_mod.minestar.containers.ContainerSkin.themedByDefault(screen) ? current() : null;
    }

    /** The current theme's art, whatever "Use the hub theme everywhere" says: for screens the container skin themes. */
    @Nullable
    public static Art current() {
        return failed ? null : get();
    }

    @Nullable
    private static synchronized Art get() {
        UiTheme theme = UiThemeState.get();
        boolean square = UiThemeState.squareCorners();
        if (cached != null && theme == cachedTheme && square == cachedSquare) return cached;
        try {
            UIElement root = new UIElement();
            root.layout(l -> l.width(200).height(200));
            Button button = new Button();
            Button disabled = new Button();
            disabled.setActive(false);
            TextField field = new TextField();
            UIElement panel = new UIElement();
            panel.addClass("panel_bg");
            for (UIElement e : new UIElement[]{button, disabled, field, panel}) {
                e.layout(l -> l.width(60).height(20));
                root.addChild(e);
            }
            ModularUI ui = ModularUI.of(UI.of(root, RollUi.sheets(), size -> size));
            ui.init(200, 200);
            Button.ButtonStyle style = button.getButtonStyle();
            IGuiTexture base = orEmpty(style.baseTexture());
            IGuiTexture hover = style.hoverTexture() != null ? style.hoverTexture() : base;
            IGuiTexture off = disabled.getButtonStyle().baseTexture() != null ? disabled.getButtonStyle().baseTexture() : base;
            cached = new Art(base, hover, off, orEmpty(field.getStyle().backgroundTexture()),
                    orEmpty(panel.getStyle().backgroundTexture()));
            cachedTheme = theme;
            cachedSquare = square;
            return cached;
        } catch (RuntimeException e) {
            // Whatever LdLib did not like, vanilla widgets simply stay vanilla (the shader still tints them).
            failed = true;
            RollMod.LOGGER.error("[Theme] could not build the theme art for vanilla widgets", e);
            return null;
        }
    }

    private static IGuiTexture orEmpty(@Nullable IGuiTexture texture) {
        return texture == null ? IGuiTexture.EMPTY : texture;
    }

    /** Draws {@code texture} over the rectangle, in vanilla's GUI pass, leaving the colour state as vanilla expects. */
    /**
     * Whether vanilla buttons should keep vanilla's sprite and take the recolour rather than the theme's button art:
     * on green OreUI, whose art is drawn from the grey Ore sheets, so a vanilla button stayed grey on a green panel.
     */
    public static boolean buttonsRecoloured() {
        UiTheme theme = UiThemeState.get();
        return theme == UiTheme.OREUI_GREEN || theme == UiTheme.ORE;
    }

    /**
     * Whether {@code screen} is one of AE2's (or an AE2 addon's: Advanced AE, Extended AE…), which "Use the hub theme
     * everywhere" leaves alone — no skin, no recolour, no themed buttons.
     */
    public static boolean isAe2(@javax.annotation.Nullable net.minecraft.client.gui.screens.Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.startsWith("appeng.") || name.startsWith("net.pedroksl.advanced_ae.")
                || name.startsWith("com.glodblock.github.extendedae.");
    }

    /** A widget that keeps its own look: neither the theme's button art nor the recolour is put on it. */
    public interface KeepsOwnLook {}

    /**
     * {@link #draw}, see-through: for the art that takes the place of vanilla's translucent backgrounds (the menu
     * background, a list's well), which let the blurred panorama or the world show behind them.
     */
    public static void drawTranslucent(GuiGraphics graphics, IGuiTexture texture, int x, int y, int w, int h, float alpha) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        draw(graphics, texture, x, y, w, h);
    }

    /** Set while a themed vanilla label draws: its text goes without a drop shadow (render thread only). */
    public static boolean noShadow;

    /** How opaque the translucent backgrounds are drawn. */
    public static final float BACKGROUND_ALPHA = 0.6f;

    public static void draw(GuiGraphics graphics, IGuiTexture texture, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        // In the theme's true colours: the recolour shader is for vanilla's grey art, not for this.
        GuiRecolor.withoutRecolor(() -> texture.draw(graphics, -1, -1, x, y, w, h, 0f));
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** A vanilla text colour as it should read on the theme's art: dark on the light themes, as given elsewhere. */
    public static int text(int argb) {
        return LightText.active() ? LightText.color(argb) : argb;
    }
}
