package com.roll_54.roll_mod.earlywindow;

import net.neoforged.fml.earlydisplay.ElementShader;
import net.neoforged.fml.earlydisplay.QuadHelper;
import net.neoforged.fml.earlydisplay.RenderElement;
import net.neoforged.fml.earlydisplay.SimpleBufferBuilder;
import net.neoforged.fml.earlydisplay.SimpleFont;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.opengl.GL32C.*;

/**
 * Everything the loading screen draws, once per frame on the early window's render thread (and, after
 * the game window takes over, inside NeoForge's loading overlay):
 * <ul>
 *   <li>the player bases, each for {@link #SLIDE_SECONDS}, crossfading into the next;</li>
 *   <li>the ModernTech logo, top middle, with the RAM in use under it;</li>
 *   <li>the shown base's name, season and builders, bottom left, sliding in from the left as each
 *       base appears (the CSS fadeInLeft: 1s ease-out, from 20px left and transparent);</li>
 *   <li>the loading bars, bottom right, in the player's hub theme.</li>
 * </ul>
 * Coordinates are the framebuffer's: 854x480 times the FML scale, origin top left.
 */
final class LoadingScene {

    private static final float SLIDE_SECONDS = 5f;
    private static final float CROSSFADE_SECONDS = 1.2f;
    private static final float TEXT_IN_SECONDS = 1f;
    private static final int MAX_BARS = 2;

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("roll_mod-earlywindow");

    private final SimpleFont font;
    private final int fontTexture;
    private final int lineSpacing;
    private final ThemeColours theme = ThemeColours.load();
    private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    private final ExecutorService decoder = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "roll_mod loading-image decoder");
        t.setDaemon(true);
        return t;
    });

    private final List<Slides.Slide> slides = Slides.loadShuffled();
    private int nextIndex;
    @Nullable private CompletableFuture<Slides.Pixels> pending;
    @Nullable private Slides.Slide pendingSlide;

    @Nullable private Shown current;
    @Nullable private Shown previous;
    private long currentSince;

    private int logoTexture = -1;
    private int logoWidth, logoHeight;

    private long memoryReadAt;
    private String memoryText = "";
    private float memoryUsed;

    /** A slide on screen: its texture and size. */
    private record Shown(Slides.Slide slide, int texture, int width, int height) {}

    LoadingScene(SimpleFont font, int fontTexture, int lineSpacing) {
        this.font = font;
        this.fontTexture = fontTexture;
        this.lineSpacing = lineSpacing;
        decodeNext();
    }

    void render(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx) {
        long now = System.nanoTime();
        advance(now);
        int s = ctx.scale();
        int w = ctx.scaledWidth();
        int h = ctx.scaledHeight();

        // The bases, the previous one under the next while they crossfade. Until the first one is up, or
        // when there are none, the theme's backdrop.
        if (current == null) drawBackdrop(bb, ctx, 1f);
        float fade = current == null ? 0f : clamp(seconds(now - currentSince) / CROSSFADE_SECONDS);
        if (previous != null) drawCover(bb, ctx, previous, 1f);
        if (current != null) drawCover(bb, ctx, current, previous == null ? 1f : fade);
        if (previous != null && fade >= 1f) {
            release(previous);
            previous = null;
        }

        int logoBottom = drawLogo(bb, ctx, s, w);
        drawMemory(bb, ctx, s, w, logoBottom + 6 * s, now);
        int captionRight = current == null ? 0 : drawCaption(bb, ctx, s, h, current.slide(), seconds(now - currentSince));
        drawBars(bb, ctx, s, w, h, captionRight);
    }

    /* ======================================================== slides ======================================================== */

    private void advance(long now) {
        if (pending == null || !pending.isDone()) return;
        boolean due = current == null || seconds(now - currentSince) >= SLIDE_SECONDS;
        if (!due) return;
        Slides.Pixels pixels = pending.getNow(null);
        Slides.Slide slide = pendingSlide;
        pending = null;
        if (slide != null) {
            // A slide whose image is missing or broken still shows: its caption over the theme backdrop.
            Shown shown = pixels == null
                    ? new Shown(slide, 0, 0, 0)
                    : new Shown(slide, upload(pixels, true), pixels.width(), pixels.height());
            if (pixels != null) pixels.free();
            if (previous != null) release(previous);
            previous = current;
            current = shown;
            currentSince = now;
        }
        // One slide is shown forever; with more, the next is decoded while this one is up.
        if (slides.size() > 1 || current == null) decodeNext();
    }

    private void decodeNext() {
        if (slides.isEmpty()) return;
        // Round the shuffled list, wrapping after the last one.
        Slides.Slide slide = slides.get(nextIndex % slides.size());
        nextIndex++;
        pendingSlide = slide;
        pending = CompletableFuture.supplyAsync(() -> Slides.decode(slide.image()), decoder)
                .exceptionally(t -> {
                    LOGGER.warn("Loading image {} could not be decoded", slide.image(), t);
                    return null;
                });
    }

    private static void release(Shown shown) {
        if (shown.texture() > 0) glDeleteTextures(shown.texture());
    }

    /** What is behind the text when there is no picture: a gradient in the theme's panel colour. */
    private void drawBackdrop(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, float alpha) {
        int top = abgr(theme.panel(), alpha);
        int bottom = abgr(darken(theme.panel(), 0.35f), alpha);
        float w = ctx.scaledWidth(), h = ctx.scaledHeight();
        ctx.elementShader().updateTextureUniform(0);
        ctx.elementShader().updateRenderTypeUniform(ElementShader.RenderType.BAR);
        bb.begin(SimpleBufferBuilder.Format.POS_TEX_COLOR, SimpleBufferBuilder.Mode.QUADS);
        bb.pos(0, 0).tex(0, 0).colour(top).endVertex();
        bb.pos(w, 0).tex(0, 0).colour(top).endVertex();
        bb.pos(0, h).tex(0, 0).colour(bottom).endVertex();
        bb.pos(w, h).tex(0, 0).colour(bottom).endVertex();
        bb.draw();
    }

    private static int darken(int argb, float factor) {
        int r = Math.round(((argb >> 16) & 0xFF) * factor);
        int g = Math.round(((argb >> 8) & 0xFF) * factor);
        int b = Math.round((argb & 0xFF) * factor);
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    /** Fills the framebuffer with the image, cropped evenly on the long side — CSS "cover". */
    private void drawCover(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, Shown shown, float alpha) {
        if (shown.texture() <= 0) {
            drawBackdrop(bb, ctx, alpha);
            return;
        }
        float canvas = (float) ctx.scaledWidth() / ctx.scaledHeight();
        float image = (float) shown.width() / shown.height();
        float u0 = 0, u1 = 1, v0 = 0, v1 = 1;
        if (image > canvas) {
            float span = canvas / image;
            u0 = (1 - span) / 2;
            u1 = u0 + span;
        } else {
            float span = image / canvas;
            v0 = (1 - span) / 2;
            v1 = v0 + span;
        }
        drawTexture(bb, ctx, shown.texture(), 0, ctx.scaledWidth(), 0, ctx.scaledHeight(), u0, u1, v0, v1,
                abgr(0xFFFFFFFF, alpha));
    }

    /* ======================================================== top ======================================================== */

    private int drawLogo(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, int s, int w) {
        if (logoTexture == -1) {
            Slides.Pixels logo = Slides.decode("logo.png");
            if (logo == null) {
                logoTexture = 0;
            } else {
                logoTexture = upload(logo, true);
                logoWidth = logo.width();
                logoHeight = logo.height();
                logo.free();
            }
        }
        int top = 14 * s;
        if (logoTexture == 0) return top;
        int width = 320 * s;
        int height = Math.round(width * (float) logoHeight / logoWidth);
        int x = (w - width) / 2;
        drawTexture(bb, ctx, logoTexture, x, x + width, top, top + height, 0, 1, 0, 1, abgr(0xFFFFFFFF, 1f));
        return top + height;
    }

    private void drawMemory(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, int s, int w, int top, long now) {
        if (now - memoryReadAt > 500_000_000L) {
            memoryReadAt = now;
            MemoryUsage heap = memory.getHeapMemoryUsage();
            long max = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
            memoryUsed = max > 0 ? (float) heap.getUsed() / max : 0f;
            memoryText = String.format("RAM %d / %d MB", heap.getUsed() >> 20, max >> 20);
        }
        int pad = 5 * s;
        int barWidth = 180 * s;
        int barHeight = 3 * s;
        int textWidth = font.stringWidth(memoryText);
        int width = Math.max(textWidth, barWidth) + 2 * pad;
        int x = (w - width) / 2;
        int height = pad + lineSpacing + 2 * s + barHeight + pad;
        panel(bb, ctx, x, top, width, height, 1f);
        text(bb, ctx, (w - textWidth) / 2, top + pad, memoryText, theme.text(), 1f);
        int barX = (w - barWidth) / 2;
        int barY = top + pad + lineSpacing + 2 * s;
        fill(bb, ctx, barX, barY, barWidth, barHeight, abgr(theme.track(), 1f));
        fill(bb, ctx, barX, barY, Math.round(barWidth * clamp(memoryUsed)), barHeight, abgr(theme.accent(), 1f));
    }

    /* ======================================================== bottom ======================================================== */

    /**
     * The base's name, then its season and builders; slides in from the left each time a base appears.
     * @return the caption's right edge once in place, 0 when there is none
     */
    private int drawCaption(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, int s, int h,
                             Slides.Slide slide, float since) {
        float t = clamp(since / TEXT_IN_SECONDS);
        float eased = 1 - (1 - t) * (1 - t) * (1 - t); // ease-out
        float alpha = eased;
        int shift = Math.round(-20 * s * (1 - eased));

        String title = slide.description();
        String meta = join(slide.date(), slide.authors());
        if (title.isEmpty() && meta.isEmpty()) return 0;
        int pad = 6 * s;
        int lines = (title.isEmpty() ? 0 : 1) + (meta.isEmpty() ? 0 : 1);
        int width = Math.max(font.stringWidth(title) + s, font.stringWidth(meta)) + 2 * pad;
        int height = lines * lineSpacing + 2 * pad;
        int x = 12 * s + shift;
        int y = h - 12 * s - height;
        panel(bb, ctx, x, y, width, height, alpha);
        int ty = y + pad;
        if (!title.isEmpty()) {
            // Bold, as the design's 18px bold: the line twice, one pixel apart.
            text(bb, ctx, x + pad, ty, title, theme.text(), alpha);
            text(bb, ctx, x + pad + s, ty, title, theme.text(), alpha);
            ty += lineSpacing;
        }
        if (!meta.isEmpty()) text(bb, ctx, x + pad, ty, meta, withAlpha(theme.text(), 0xB0), alpha);
        return 12 * s + width;
    }

    /** The loading bars, bottom right; narrower when the caption, ending at {@code captionRight}, is in the way. */
    private void drawBars(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, int s, int w, int h, int captionRight) {
        List<ProgressMeter> meters = StartupNotificationManager.getCurrentProgress();
        int count = Math.min(MAX_BARS, meters.size());
        if (count == 0) return;
        int pad = 6 * s;
        int left = captionRight > 0 ? captionRight + 8 * s : 12 * s;
        int barWidth = Math.max(60 * s, Math.min(260 * s, w - 12 * s - left - 2 * pad));
        int barHeight = 6 * s;
        int row = lineSpacing + 2 * s + barHeight + 4 * s;
        int width = barWidth + 2 * pad;
        int height = count * row - 4 * s + 2 * pad;
        int x = w - 12 * s - width;
        int y = h - 12 * s - height;
        panel(bb, ctx, x, y, width, height, 1f);
        for (int i = 0; i < count; i++) {
            ProgressMeter meter = meters.get(i);
            int top = y + pad + i * row;
            text(bb, ctx, x + pad, top, clip(meter.label().getText(), barWidth), theme.text(), 1f);
            int barY = top + lineSpacing + 2 * s;
            int barX = x + pad;
            fill(bb, ctx, barX, barY, barWidth, barHeight, abgr(theme.track(), 1f));
            if (meter.steps() == 0) {
                // No step count: a block sweeping across, as NeoForge's own bar does.
                float phase = (System.nanoTime() / 1_000_000L % 2000L) / 2000f;
                int block = barWidth / 5;
                int bx = Math.round((barWidth + block) * phase) - block;
                int from = Math.max(0, bx);
                int to = Math.min(barWidth, bx + block);
                if (to > from) fill(bb, ctx, barX + from, barY, to - from, barHeight, abgr(theme.accent(), 1f));
            } else {
                fill(bb, ctx, barX, barY, Math.round(barWidth * clamp(meter.progress())), barHeight, abgr(theme.accent(), 1f));
            }
        }
    }

    /* ======================================================== drawing ======================================================== */

    /** A theme panel with its hairline, slightly see-through so the base shows behind it. */
    private void panel(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, int x, int y, int w, int h, float alpha) {
        int s = ctx.scale();
        fill(bb, ctx, x - s, y - s, w + 2 * s, h + 2 * s, abgr(theme.line(), alpha));
        fill(bb, ctx, x, y, w, h, abgr(withAlpha(theme.panel(), 0xD8), alpha));
    }

    private void fill(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, int x, int y, int w, int h, int abgr) {
        if (w <= 0 || h <= 0) return;
        ctx.elementShader().updateTextureUniform(0);
        ctx.elementShader().updateRenderTypeUniform(ElementShader.RenderType.BAR);
        bb.begin(SimpleBufferBuilder.Format.POS_TEX_COLOR, SimpleBufferBuilder.Mode.QUADS);
        QuadHelper.loadQuad(bb, x, x + w, y, y + h, 0f, 0f, 0f, 0f, abgr);
        bb.draw();
    }

    private void text(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, int x, int y, String text, int argb, float alpha) {
        if (text.isEmpty()) return;
        ctx.elementShader().updateTextureUniform(fontTexture);
        ctx.elementShader().updateRenderTypeUniform(ElementShader.RenderType.FONT);
        bb.begin(SimpleBufferBuilder.Format.POS_TEX_COLOR, SimpleBufferBuilder.Mode.QUADS);
        font.generateVerticesForTexts(x, y, bb, new SimpleFont.DisplayText(text, abgr(argb, alpha)));
        bb.draw();
    }

    private void drawTexture(SimpleBufferBuilder bb, RenderElement.DisplayContext ctx, int texture,
                             float x0, float x1, float y0, float y1, float u0, float u1, float v0, float v1, int abgr) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        ctx.elementShader().updateTextureUniform(0);
        ctx.elementShader().updateRenderTypeUniform(ElementShader.RenderType.TEXTURE);
        bb.begin(SimpleBufferBuilder.Format.POS_TEX_COLOR, SimpleBufferBuilder.Mode.QUADS);
        QuadHelper.loadQuad(bb, x0, x1, y0, y1, u0, u1, v0, v1, abgr);
        bb.draw();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /** Uploads decoded pixels as a texture, smoothly filtered (mipmapped, as photos are drawn smaller). */
    private static int upload(Slides.Pixels pixels, boolean mipmaps) {
        int texture = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, mipmaps ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, pixels.width(), pixels.height(), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels.data());
        if (mipmaps) glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        return texture;
    }

    /* ======================================================== helpers ======================================================== */

    /** ARGB, its alpha scaled by {@code alpha}, packed the way the early window's buffers take it (ABGR). */
    private static int abgr(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * clamp(alpha));
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return a << 24 | b << 16 | g << 8 | r;
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha & 0xFF) << 24 | (argb & 0xFFFFFF);
    }

    private String clip(String text, int width) {
        if (font.stringWidth(text) <= width) return text;
        String dots = "...";
        int end = text.length();
        while (end > 0 && font.stringWidth(text.substring(0, end) + dots) > width) end--;
        return text.substring(0, end) + dots;
    }

    private static String join(String date, String authors) {
        if (date.isEmpty()) return authors;
        if (authors.isEmpty()) return date;
        return date + "  -  " + authors;
    }

    private static float seconds(long nanos) {
        return nanos / 1_000_000_000f;
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }
}
