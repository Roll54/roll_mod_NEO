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
    private static final java.util.Set<String> OWN_COLOURS = java.util.Set.of();

    public static boolean keepsOwnColours(@Nullable net.minecraft.resources.ResourceLocation texture) {
        if (texture == null) return false;
        // Atlases of pictures, not of GUI frames: potion effect icons (mob_effects), and items or blocks drawn as flat
        // sprites (TEI's item icons, from the block atlas). Their greys are the picture's own: recoloured, a diamond
        // pickaxe's iron or a Slowness icon turned into the theme's dark tones.
        if (texture.getPath().startsWith("textures/atlas/") && !texture.getPath().equals("textures/atlas/gui.png")) return true;
        return OWN_COLOURS.contains(texture.getNamespace());
    }

    /**
     * {@code argb} as the recolour shader would draw it: an opaque grey mapped through the theme's five tones, anything
     * coloured or see-through as it is. For what is not a texture blit: other mods' panels and frames drawn with
     * {@code fill} (Extreme Sound Muffler's side panel, many mods' grey boxes) take the theme like their textures do.
     */
    public static int fillColor(int argb) {
        UiTheme.Recolor recolor = active();
        if (recolor == null || (argb >>> 24) != 0xFF) return argb;
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int hi = Math.max(r, Math.max(g, b)), lo = Math.min(r, Math.min(g, b));
        if (hi - lo > 10) return argb;
        float v = hi / 255f;
        int mapped;
        if (v < 0.333f) mapped = mix(recolor.outline(), recolor.shadow(), v / 0.333f);
        else if (v < 0.545f) mapped = mix(recolor.shadow(), recolor.well(), (v - 0.333f) / 0.212f);
        else if (v < 0.776f) mapped = mix(recolor.well(), recolor.surface(), (v - 0.545f) / 0.231f);
        else mapped = mix(recolor.surface(), recolor.highlight(), (v - 0.776f) / 0.224f);
        return 0xFF000000 | (mapped & 0xFFFFFF);
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * The colour for text a screen writes in {@code rgb}. On the screens of {@link #BLACK_TEXT_ON_PANEL} mods, black
     * and near-black text sits on their grey panel, which the recolour darkens: there it takes the theme's text colour.
     * Everywhere else black is kept — a book's page or a sign is not recoloured, and its black text must stay black.
     */
    public static int textColor(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int hi = Math.max(r, Math.max(g, b)), lo = Math.min(r, Math.min(g, b));
        if (hi > 0x40 || hi - lo > 8) return rgb;
        UiTheme.Recolor recolor = active();
        if (recolor == null || !blackTextOnPanel(Minecraft.getInstance().screen)) return rgb;
        return recolor.text() & 0xFFFFFF;
    }

    /** Packages of mods whose screens write black text straight on their grey (recoloured) panel. */
    private static final java.util.List<String> BLACK_TEXT_ON_PANEL = java.util.List.of("com.leobeliik.extremesoundmuffler.");

    private static boolean blackTextOnPanel(@Nullable Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        for (String prefix : BLACK_TEXT_ON_PANEL) {
            if (name.startsWith(prefix)) return true;
        }
        return false;
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

    private static final net.minecraft.resources.ResourceLocation TEI_BUTTONS =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tei", "textures/gui/buttons.png");
    private static final net.minecraft.resources.ResourceLocation TEI_BUTTONS_THEMED =
            RollMod.id("textures/gui/tei/buttons.png");

    /**
     * A texture to draw in place of {@code texture} while the recolour is on: TEI's button sheet, whose green
     * green "on" states the recolour cannot touch, in the player's notification colour ({@link TeiButtonSheet}).
     */
    public static net.minecraft.resources.ResourceLocation swapTexture(net.minecraft.resources.ResourceLocation texture) {
        return TEI_BUTTONS.equals(texture) && active() != null ? TeiButtonSheet.texture() : texture;
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
