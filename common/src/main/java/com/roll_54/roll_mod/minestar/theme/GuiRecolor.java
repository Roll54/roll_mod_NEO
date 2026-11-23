package com.roll_54.roll_mod.minestar.theme;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Experimental: vanilla's screens — inventory, chests, furnaces, buttons, every container that
 * copies vanilla's grey art — redrawn in the hub's theme when "Use the hub theme everywhere" is on.
 *
 * <p>Vanilla screens have no theme to switch: they are fixed PNGs in a few greys. So the texture
 * itself is left alone, and the <em>drawing</em> of it is recoloured: every GUI blit goes through
 * {@code GuiGraphics.innerBlit}, which picks vanilla's {@code position_tex} or
 * {@code position_tex_color} shader; the client module's {@code GuiGraphicsRecolorMixin} swaps in
 * this class's copies of those two, which map each grey pixel to the theme's tones (see
 * {@link UiTheme.Recolor} and {@code shaders/core/gui_recolor.fsh}) and leave anything coloured
 * as it is. Container titles, which vanilla writes in a fixed dark grey, take the theme's text
 * colour ({@code AbstractContainerScreenLabelMixin}).
 *
 * <p>On every vanilla screen — in a world and out of it, so the title screen and its menus too — but
 * never on the HUD (no screen open) and never on LdLib's own screens, which are themed properly
 * already ({@link ForceThemeHandler}).
 *
 * <p>TODO(theme): option 2 from the design discussion — instead of recolouring at draw time,
 * generate a recoloured copy of vanilla's GUI textures into a built-in resource pack per theme and
 * enable it. Exact and shader-free, but a resource reload on every theme change, and modded screens
 * would only be covered if their textures were added to the pack too. Worth it if the shader route
 * shows artefacts on some screens.
 */
@OnlyIn(Dist.CLIENT)
// RegisterShadersEvent is a mod-bus event; NeoForge routes it there on its own.
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class GuiRecolor {

    @Nullable private static ShaderInstance tex;
    @Nullable private static ShaderInstance texColor;

    private GuiRecolor() {}

    /**
     * Loads the two recolouring shaders. A shader that fails to load or compile — a driver that
     * rejects it, a resource pack that breaks it — is logged and left out: the feature then simply
     * does nothing ({@link #pick} falls back to vanilla's), rather than the game failing to start.
     */
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), RollMod.id("gui_recolor"),
                    DefaultVertexFormat.POSITION_TEX), shader -> tex = shader);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), RollMod.id("gui_recolor_color"),
                    DefaultVertexFormat.POSITION_TEX_COLOR), shader -> texColor = shader);
        } catch (IOException | RuntimeException e) {
            tex = null;
            texColor = null;
            RollMod.LOGGER.error("[Theme] the GUI recolour shaders could not be loaded; vanilla screens stay vanilla", e);
        }
    }

    /**
     * The shader a GUI blit should use: this class's recolouring one while the feature is on and a
     * vanilla screen is up, otherwise {@code vanilla} untouched.
     *
     * @param tinted whether the blit is the vertex-coloured one ({@code position_tex_color})
     */
    public static Supplier<ShaderInstance> pick(Supplier<ShaderInstance> vanilla, boolean tinted) {
        UiTheme.Recolor recolor = active();
        ShaderInstance shader = tinted ? texColor : tex;
        if (recolor == null || shader == null) return vanilla;
        // The palette goes to the shader only when it changed (or the shader was reloaded), not on every blit.
        if (tinted ? recolor != texColorPalette || shader != texColorShader : recolor != texPalette || shader != texShader) {
            set(shader, "RecolorOutline", recolor.outline());
            set(shader, "RecolorShadow", recolor.shadow());
            set(shader, "RecolorWell", recolor.well());
            set(shader, "RecolorSurface", recolor.surface());
            set(shader, "RecolorHighlight", recolor.highlight());
            if (tinted) {
                texColorPalette = recolor;
                texColorShader = shader;
            } else {
                texPalette = recolor;
                texShader = shader;
            }
        }
        return tinted ? TEX_COLOR : TEX;
    }

    /**
     * Mods whose GUI art is drawn as it is, never recoloured: their icons are mostly grey, and mapped into the theme's
     * dark tones they vanished into the panel (Extreme Sound Muffler's speaker button in the inventory, the play and
     * mute buttons on its screen).
     */
    private static final java.util.Set<String> OWN_COLOURS = java.util.Set.of("extremesoundmuffler");

    public static boolean keepsOwnColours(@Nullable net.minecraft.resources.ResourceLocation texture) {
        return texture != null && OWN_COLOURS.contains(texture.getNamespace());
    }

    /** The palette each shader last got, and which shader instance it went to. */
    @Nullable private static UiTheme.Recolor texPalette, texColorPalette;
    @Nullable private static ShaderInstance texShader, texColorShader;
    private static final Supplier<ShaderInstance> TEX = () -> tex;
    private static final Supplier<ShaderInstance> TEX_COLOR = () -> texColor;

    /** The colour for a label vanilla writes in its fixed dark grey: the theme's text while active. */
    public static int labelColor(int vanilla) {
        UiTheme.Recolor recolor = active();
        return recolor == null ? vanilla : recolor.text() & 0xFFFFFF;
    }

    /**
     * The sprite to draw in place of {@code sprite}. The world and server lists' hovered buttons (play, move up and
     * down, delete) are drawn by vanilla in blue: coloured, so the recolour left them blue on every theme. While the
     * recolour is on they are swapped for the plain grey ones, which it maps into the theme's tones; the hovered row
     * still shows through vanilla's highlight over its icon.
     */
    public static net.minecraft.resources.ResourceLocation sprite(net.minecraft.resources.ResourceLocation sprite) {
        String path = sprite.getPath();
        if (!path.endsWith("_highlighted") || !"minecraft".equals(sprite.getNamespace())) return sprite;
        if (!path.startsWith("world_list/") && !path.startsWith("server_list/") && !path.startsWith("transferable_list/")) {
            return sprite;
        }
        if (active() == null) return sprite;
        return sprite.withPath(path.substring(0, path.length() - "_highlighted".length()));
    }

    /** How deep we are in drawing the theme's own art (its panel, buttons, fields), which must not be recoloured. */
    private static int themeArtDepth;

    /** Runs {@code draw} with the recolouring off: the theme's own art is drawn in its true colours. Render thread only. */
    public static void withoutRecolor(Runnable draw) {
        themeArtDepth++;
        try {
            draw.run();
        } finally {
            themeArtDepth--;
        }
    }

    /**
     * Whether the HUD (hotbar, hearts, armour, food) is being drawn. The HUD is drawn in the same frame as an open
     * screen, before it — so with chat or the inventory open, a check on {@code Minecraft.screen} alone recoloured the
     * HUD too, and the hearts' and hotbar's greys changed whenever a screen opened. Set by the client's
     * {@code GuiHudNoRecolorMixin}. Render thread only.
     */
    private static boolean drawingHud;

    public static void setDrawingHud(boolean hud) {
        drawingHud = hud;
    }

    /** Whether vanilla's GUI art is being recoloured right now. */
    public static boolean on() {
        return active() != null;
    }

    /** The recolour to apply right now, or null when vanilla should draw as it always has. */
    @Nullable
    private static UiTheme.Recolor active() {
        if (themeArtDepth > 0 || drawingHud) return null;
        Screen screen = Minecraft.getInstance().screen;
        // The rest depends only on the screen and the theme settings: worked out once for each, not per blit.
        if (screen == cachedScreen && UiThemeState.version() == cachedVersion) return cachedRecolor;
        cachedScreen = screen;
        cachedVersion = UiThemeState.version();
        cachedRecolor = forScreen(screen);
        return cachedRecolor;
    }

    @Nullable private static Screen cachedScreen;
    private static int cachedVersion = -1;
    @Nullable private static UiTheme.Recolor cachedRecolor;

    @Nullable
    private static UiTheme.Recolor forScreen(@Nullable Screen screen) {
        if (screen == null || ThemeArt.isAe2(screen)) return null;
        // Only with "Use the hub theme everywhere" on: off, every vanilla and mod screen stays as it is.
        if (!UiThemeState.forceEverywhere()) return null;
        if (screen instanceof ModularUIContainerScreen || screen instanceof ModularUIScreen) return null;
        // A container the LdLib skin draws whole is themed already. On an overlay-skinned one only the background was
        // replaced: what the screen still draws itself (arrows, progress bars, the enchanting book, a mod's own
        // widgets) is recoloured here automatically, so it matches the theme's panel without any new textures.
        if (com.roll_54.roll_mod.minestar.containers.ContainerSkin.isFullSkin(screen)) return null;
        return UiThemeState.get().recolor();
    }

    private static void set(ShaderInstance shader, String name, int argb) {
        shader.safeGetUniform(name).set(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f, 1f);
    }
}
