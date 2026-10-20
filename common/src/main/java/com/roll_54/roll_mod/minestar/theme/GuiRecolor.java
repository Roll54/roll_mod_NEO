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
 * <p>Only while a screen is open in a world — so not the title screen, its logo or the HUD — and
 * never on LdLib's own screens, which are themed properly already ({@link ForceThemeHandler}).
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
        set(shader, "RecolorOutline", recolor.outline());
        set(shader, "RecolorShadow", recolor.shadow());
        set(shader, "RecolorWell", recolor.well());
        set(shader, "RecolorSurface", recolor.surface());
        set(shader, "RecolorHighlight", recolor.highlight());
        return () -> shader;
    }

    /** The colour for a label vanilla writes in its fixed dark grey: the theme's text while active. */
    public static int labelColor(int vanilla) {
        UiTheme.Recolor recolor = active();
        return recolor == null ? vanilla : recolor.text() & 0xFFFFFF;
    }

    /** The recolour to apply right now, or null when vanilla should draw as it always has. */
    @Nullable
    private static UiTheme.Recolor active() {
        if (!UiThemeState.forceEverywhere()) return null;
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (mc.level == null || screen == null) return null;
        if (screen instanceof ModularUIContainerScreen || screen instanceof ModularUIScreen) return null;
        return UiThemeState.get().recolor();
    }

    private static void set(ShaderInstance shader, String name, int argb) {
        shader.safeGetUniform(name).set(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f, 1f);
    }
}
