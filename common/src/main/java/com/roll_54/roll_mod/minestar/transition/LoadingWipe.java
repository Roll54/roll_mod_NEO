package com.roll_54.roll_mod.minestar.transition;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * The halftone wipe around loading:
 * <ol>
 *   <li>a bold diagonal band, #b7043b, with halftone-dot edges sweeps from the upper left to the lower
 *       right, leaving black behind it ({@link #WIPE_SECONDS});</li>
 *   <li>that black interstitial holds, the ModernTech logo small in its middle, for at least
 *       {@link #MIN_HOLD_SECONDS} — and while loading goes on past that, three meteorite tools under the
 *       logo bob in a wave, each lighting up as its third of the loading is done;</li>
 *   <li>the same wipe sweeps on and reveals the next screen — the main menu, or the world.</li>
 * </ol>
 * Two ways in:
 * <ul>
 *   <li><b>The game's startup</b> ({@code NeoForgeLoadingOverlayMixin}): roll_mod's early window shows
 *       the loading; the wipe starts when it is done. The first sweep is drawn over NeoForge's own
 *       frame, which still shows the loading screen unchanged for a second after loading ends.</li>
 *   <li><b>Resource reloads</b> ({@code LoadingOverlayMixin}, vanilla's Mojang screen — also the startup
 *       when NeoForge's early window is off): replaced whole, from its first frame. The first sweep goes
 *       over what was on screen, and the interstitial is the loading screen.</li>
 * </ul>
 * The wipe is one shader ({@code shaders/core/loading_wipe.fsh}) over the whole window; nothing under
 * it is changed. If the shader failed to load, nothing is taken over and loading looks as in vanilla.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class LoadingWipe {

    /** One sweep. Under a second: NeoForge's overlay begins its own fade a second after loading ends. */
    private static final float WIPE_SECONDS = 0.8f;
    /** The shortest hold of the interstitial; loading still going on then shows the tools. */
    private static final float MIN_HOLD_SECONDS = 2.0f;
    /** The tools fade in and out this fast. */
    private static final float TOOLS_FADE_SECONDS = 0.3f;

    private static final int BAND_COLOR = 0xB7043B;
    private static final ResourceLocation LOGO = RollMod.id("textures/gui/title/moderntech_logo.png");
    /** The logo's pixel size, for its aspect ratio. */
    private static final float LOGO_ASPECT = 2048f / 293f;
    /** The logo's width: this share of the window, at most {@link #LOGO_MAX_GUI_WIDTH} GUI pixels. */
    private static final float LOGO_SHARE = 0.3f;
    private static final float LOGO_MAX_GUI_WIDTH = 320f;

    /**
     * Pickaxe, axe, shovel. Animated strips of 16x16 frames, each shown 10 ticks and blended into the
     * next (their .mcmeta); drawn straight from the texture files, as the atlases may be mid-reload.
     */
    private static final ResourceLocation[] TOOLS = {
            RollMod.id("textures/item/tool/meteorite_pickaxe.png"),
            RollMod.id("textures/item/tool/meteorite_axe.png"),
            RollMod.id("textures/item/tool/meteorite_shovel.png")};
    private static final int TOOL_FRAMES = 13;
    private static final float TOOL_FRAME_SECONDS = 0.5f;
    /** A tool's size on screen, GUI pixels: two per texel. */
    private static final int TOOL_SIZE = 32;
    private static final int TOOL_GAP = 12;
    /** How high a tool bobs, GUI pixels, and how long one wave takes. */
    private static final float TOOL_BOB = 7f;
    private static final float TOOL_BOB_SECONDS = 1.1f;
    /** A tool's brightness before its share of the loading is done. */
    private static final float TOOL_DIM = 0.3f;

    @Nullable private static ShaderInstance shader;

    private enum Mode {
        /** Startup: the wipe starts once loading is done; NeoForge draws its loading screen meanwhile. */
        AFTER_LOADING,
        /** A reload: the wipe is the whole loading screen, from its first frame. */
        WHOLE_LOADING
    }

    /** The overlay the running wipe belongs to; null when none runs. */
    @Nullable private static Object owner;
    private static Mode mode = Mode.AFTER_LOADING;
    private static long startMillis;
    /** When loading was done, ms; -1 while it goes on. */
    private static long doneMillis = -1;
    private static boolean revealing;

    /** What {@link #takeOver} did with a frame. */
    public enum Frame {
        /** Nothing: let the overlay draw. */
        OVERLAY,
        /** Drawn here instead of the overlay. */
        DRAWN,
        /** Drawn, and the wipe is over: end the overlay now. */
        FINISHED
    }

    private LoadingWipe() {}

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), RollMod.id("loading_wipe"),
                    DefaultVertexFormat.POSITION), loaded -> shader = loaded);
        } catch (IOException | RuntimeException e) {
            shader = null;
            RollMod.LOGGER.error("[LoadingWipe] the wipe shader could not be loaded; loading screens stay vanilla", e);
        }
    }

    /* ======================================================== state ======================================================== */

    /** Startup: {@code overlay}'s loading has just ended. Again for the same overlay does nothing. */
    public static void beginAfterLoading(Object overlay) {
        if (!start(overlay, Mode.AFTER_LOADING)) return;
        doneMillis = startMillis;
    }

    /**
     * A reload: {@code overlay} is about to draw its first frame. Again for the same overlay does nothing.
     * @return whether a wipe runs for it — false if the shader is unavailable
     */
    public static boolean beginWholeLoading(Object overlay) {
        start(overlay, Mode.WHOLE_LOADING);
        return owner == overlay;
    }

    private static boolean start(Object overlay, Mode newMode) {
        if (shader == null || owner == overlay) return false;
        owner = overlay;
        mode = newMode;
        startMillis = Util.getMillis();
        doneMillis = -1;
        revealing = false;
        RollMod.LOGGER.debug("[LoadingWipe] {} over {}", newMode, overlay.getClass().getSimpleName());
        return true;
    }

    /**
     * Whether {@code overlay}'s loading may be finished now (the screens it opens set up): once the first
     * sweep has covered the window, so nothing changes in view.
     */
    public static boolean isCovered(Object overlay) {
        return owner == overlay && seconds() >= WIPE_SECONDS;
    }

    /** {@code overlay}'s loading is done; the hold ends at its minimum or now, whichever is later. */
    public static void loadingDone(Object overlay) {
        if (owner == overlay && doneMillis == -1) doneMillis = Util.getMillis();
    }

    /** Whether a wipe is covering the screen — from its start until the reveal begins. */
    public static boolean isCovering() {
        return owner != null && !revealing;
    }

    /* ======================================================== frames ======================================================== */

    /** Startup's first sweep, drawn over NeoForge's own frame. */
    public static void drawOverOverlay(GuiGraphics graphics, Object overlay) {
        if (owner != overlay || mode != Mode.AFTER_LOADING || shader == null) return;
        float t = seconds();
        if (t < WIPE_SECONDS) draw(graphics, false, ease(t / WIPE_SECONDS));
    }

    /**
     * The frame, when the wipe draws it: the first sweep of a reload over what was on screen, the hold,
     * and the next screen under the reveal. On {@link Frame#FINISHED} the caller ends its overlay.
     *
     * @param progress how far loading is, 0..1, for the tools
     */
    public static Frame takeOver(GuiGraphics graphics, Object overlay, int mouseX, int mouseY, float partialTick,
                                 float progress) {
        if (owner != overlay) return Frame.OVERLAY;
        if (shader == null) {
            owner = null;
            return Frame.OVERLAY;
        }
        Minecraft mc = Minecraft.getInstance();
        float t = seconds();
        if (t < WIPE_SECONDS) {
            if (mode == Mode.AFTER_LOADING) return Frame.OVERLAY;
            // What was on screen; the world and HUD, if any, were drawn under the overlay already.
            if (mc.screen != null) ClientHooks.drawScreen(mc.screen, graphics, mouseX, mouseY, partialTick);
            draw(graphics, false, ease(t / WIPE_SECONDS));
            return Frame.DRAWN;
        }
        float done = doneMillis == -1 ? Float.NaN : (doneMillis - startMillis) / 1000f;
        float toolsFrom = WIPE_SECONDS + MIN_HOLD_SECONDS;
        // Done before the minimum: no tools. Done after: they fade out, then the reveal.
        boolean toolsShown = Float.isNaN(done) || done > toolsFrom;
        float holdEnd = Float.isNaN(done) ? Float.MAX_VALUE
                : Math.max(toolsFrom, done + (toolsShown ? TOOLS_FADE_SECONDS : 0f));
        if (t < holdEnd) {
            draw(graphics, false, 1f);
            if (toolsShown && t > toolsFrom) {
                float alpha = clamp((t - toolsFrom) / TOOLS_FADE_SECONDS);
                if (!Float.isNaN(done)) alpha = Math.min(alpha, 1f - clamp((t - done) / TOOLS_FADE_SECONDS));
                drawTools(graphics, t, Float.isNaN(done) ? progress : 1f, alpha);
            }
            return Frame.DRAWN;
        }

        revealing = true;
        // The next screen, as the overlays draw it during their own fade.
        if (mc.screen != null) ClientHooks.drawScreen(mc.screen, graphics, mouseX, mouseY, partialTick);
        float reveal = (t - holdEnd) / WIPE_SECONDS;
        if (reveal < 1f) {
            draw(graphics, true, ease(reveal));
            return Frame.DRAWN;
        }
        owner = null;
        return Frame.FINISHED;
    }

    /* ======================================================== wipe ======================================================== */

    /**
     * The wipe over the whole window. {@code progress} 0..1 moves the band from just before the upper
     * left corner to just past the lower right; covering, the interstitial is left behind it, revealing,
     * what is under it.
     */
    private static void draw(GuiGraphics graphics, boolean reveal, float progress) {
        ShaderInstance wipe = shader;
        if (wipe == null) return;
        graphics.flush();
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        int fbWidth = window.getWidth();
        int fbHeight = window.getHeight();
        if (fbWidth <= 0 || fbHeight <= 0) return;

        float spacing = Math.max(8f, fbHeight / 45f);
        float halo = 4f * spacing;
        float sweep = (fbWidth + fbHeight) * 0.70710678f; // the sweep axis is 45 degrees
        float band = 0.12f * sweep;
        float from = reveal ? -(band + halo) : -halo;
        float to = reveal ? sweep + halo : sweep + band + halo;
        float front = from + (to - from) * progress;
        float[] logo = logoRect(window);

        wipe.safeGetUniform("ScreenSize").set((float) fbWidth, (float) fbHeight);
        wipe.safeGetUniform("Front").set(front);
        wipe.safeGetUniform("Band").set(band);
        wipe.safeGetUniform("Halo").set(halo);
        wipe.safeGetUniform("Spacing").set(spacing);
        wipe.safeGetUniform("Reveal").set(reveal ? 1f : 0f);
        wipe.safeGetUniform("BandColor").set(((BAND_COLOR >> 16) & 0xFF) / 255f, ((BAND_COLOR >> 8) & 0xFF) / 255f,
                (BAND_COLOR & 0xFF) / 255f, 1f);
        wipe.safeGetUniform("LogoRect").set(logo[0], logo[1], logo[2], logo[3]);

        // The logo is shown far smaller than drawn: smooth filtering for it, for this draw only.
        AbstractTexture logoTexture = mc.getTextureManager().getTexture(LOGO);
        logoTexture.setFilter(true, false);
        RenderSystem.setShaderTexture(0, LOGO);
        RenderSystem.setShader(() -> wipe);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        // The whole window, in clip space: NeoForge's overlay leaves a pixel projection of its own set
        // when its frame ends, so GUI coordinates would cover only part of the window there.
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(-1f, -1f, 0f);
        buffer.addVertex(1f, -1f, 0f);
        buffer.addVertex(1f, 1f, 0f);
        buffer.addVertex(-1f, 1f, 0f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        logoTexture.setFilter(false, false);
    }

    /** The logo's place in framebuffer pixels: x, y, width, height — centred. */
    private static float[] logoRect(Window window) {
        int fbWidth = window.getWidth();
        int fbHeight = window.getHeight();
        float width = Math.min(fbWidth * LOGO_SHARE, LOGO_MAX_GUI_WIDTH * (float) window.getGuiScale());
        float height = width / LOGO_ASPECT;
        return new float[] {(fbWidth - width) / 2f, (fbHeight - height) / 2f, width, height};
    }

    /* ======================================================== tools ======================================================== */

    /**
     * The three tools under the logo: bobbing one after another, each from dim to full brightness as its
     * third of {@code progress} is reached; the whole row at {@code alpha}.
     */
    private static void drawTools(GuiGraphics graphics, float t, float progress, float alpha) {
        if (alpha <= 0f) return;
        graphics.flush();
        Window window = Minecraft.getInstance().getWindow();
        float guiScale = (float) window.getGuiScale();
        float[] logo = logoRect(window);
        float logoBottom = (logo[1] + logo[3]) / guiScale;
        int rowWidth = TOOLS.length * TOOL_SIZE + (TOOLS.length - 1) * TOOL_GAP;
        float x0 = (graphics.guiWidth() - rowWidth) / 2f;
        float y0 = logoBottom + 14f + TOOL_BOB;

        // Each frame blends into the next, as the .mcmeta's "interpolate" does in game.
        float frames = t / TOOL_FRAME_SECONDS;
        int frame = (int) Math.floor(frames) % TOOL_FRAMES;
        int next = (frame + 1) % TOOL_FRAMES;
        float blend = frames - (float) Math.floor(frames);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        Matrix4f pose = graphics.pose().last().pose();
        for (int i = 0; i < TOOLS.length; i++) {
            float phase = (float) (2 * Math.PI * t / TOOL_BOB_SECONDS - i * 0.8);
            float y = y0 - TOOL_BOB * Math.max(0f, (float) Math.sin(phase));
            float x = x0 + i * (TOOL_SIZE + TOOL_GAP);
            float lit = clamp(progress * TOOLS.length - i);
            float shade = TOOL_DIM + (1f - TOOL_DIM) * lit;
            RenderSystem.setShaderTexture(0, TOOLS[i]);
            quad(pose, x, y, frame, shade, alpha);
            quad(pose, x, y, next, shade, alpha * blend);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /** One frame of a tool strip. Straight to the tesselator: the GUI's blits may be recoloured by the theme. */
    private static void quad(Matrix4f pose, float x, float y, int frame, float shade, float alpha) {
        if (alpha <= 0f) return;
        float v0 = frame / (float) TOOL_FRAMES;
        float v1 = (frame + 1) / (float) TOOL_FRAMES;
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(pose, x, y, 0f).setUv(0f, v0).setColor(shade, shade, shade, alpha);
        buffer.addVertex(pose, x, y + TOOL_SIZE, 0f).setUv(0f, v1).setColor(shade, shade, shade, alpha);
        buffer.addVertex(pose, x + TOOL_SIZE, y + TOOL_SIZE, 0f).setUv(1f, v1).setColor(shade, shade, shade, alpha);
        buffer.addVertex(pose, x + TOOL_SIZE, y, 0f).setUv(1f, v0).setColor(shade, shade, shade, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /* ======================================================== helpers ======================================================== */

    private static float seconds() {
        return (Util.getMillis() - startMillis) / 1000f;
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }

    /** Cubic ease in and out. */
    private static float ease(float t) {
        t = clamp(t);
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }
}
