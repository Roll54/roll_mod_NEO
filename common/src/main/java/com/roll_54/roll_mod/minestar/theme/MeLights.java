package com.roll_54.roll_mod.minestar.theme;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * The AE2 theme's ME controller colours — green, yellow, orange, red, pink, purple, blue, cyan — flowing along tab
 * titles the way the lights cycle on the block ({@link #title}). Switched off in Settings ({@link UiThemeState#meLights}). Drawn as an element's background along its bottom edge (the hub tab headers), only while
 * the AE2 theme is on and the player has not switched it off in Settings ({@link UiThemeState#meLights}); otherwise
 * it draws nothing. Read every frame, so changing either applies at once.
 */
public final class MeLights implements IGuiTexture {

    /**
     * One shared instance: it holds no state, the phase comes from the clock.
     * @deprecated The line under tab titles was replaced by the titles themselves shimmering ({@link #title}).
     */
    @Deprecated
    public static final MeLights STRIP = new MeLights();

    /**
     * The colours an ME controller's lights run through (AE2's {@code controller_lights.png}, its 12 frames in order),
     * each shown for 25 ticks and blended into the next, as the block's animation does.
     */
    private static final int[] FRAMES = {
            0xB3F86D, 0xF4FF80, 0xF2BA49, 0x8E6E1A, 0xF07665, 0xFAB3C7,
            0xD5719C, 0xD878DA, 0x3CABFF, 0x74E8FF, 0x65E8C9, 0x23C660};
    /** One frame: 25 ticks. */
    private static final float FRAME_MS = 1250f;
    /** How many frames apart the two ends of the line are, so the colours run along it rather than all change at once. */
    private static final float SPREAD_FRAMES = 3f;

    private MeLights() {}

    public static boolean active() {
        return UiThemeState.get() == UiTheme.AE2 && UiThemeState.meLights();
    }

    @Override
    public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height,
                     float partialTicks) {
        if (width < 2 || !active()) return;
        int left = Mth.floor(x);
        int right = Mth.floor(x + width);
        int bottom = Mth.floor(y + height);
        float now = (System.currentTimeMillis() % (long) (FRAME_MS * FRAMES.length)) / FRAME_MS;
        int span = right - left;
        for (int i = 0; i < span; i++) {
            // Further along the line is further back in the cycle: the colours flow left to right.
            float frame = now - SPREAD_FRAMES * i / span;
            int rgb = colourAt(frame);
            graphics.fill(left + i, bottom - 1, left + i + 1, bottom, 0xFF000000 | rgb);
            graphics.fill(left + i, bottom - 2, left + i + 1, bottom - 1, 0x55000000 | rgb);
        }
    }

    /**
     * {@code text} with each letter in the controller's colour for this moment, the colours running along the word —
     * how the AE2 theme shows tab titles. Formatting the text already had (bold) is kept; its colour is replaced.
     */
    public static net.minecraft.network.chat.Component title(net.minecraft.network.chat.Component text) {
        String plain = text.getString();
        net.minecraft.network.chat.MutableComponent out = net.minecraft.network.chat.Component.empty();
        float now = (System.currentTimeMillis() % (long) (FRAME_MS * FRAMES.length)) / FRAME_MS;
        int n = Math.max(1, plain.codePointCount(0, plain.length()));
        int[] i = {0};
        plain.codePoints().forEach(cp -> {
            float frame = now - SPREAD_FRAMES * i[0]++ / n;
            out.append(net.minecraft.network.chat.Component.literal(Character.toString(cp))
                    .withStyle(text.getStyle().withColor(colourAt(frame)).withInsertion(LightText.GLOW)));
        });
        return out;
    }

    /**
     * A 1-pixel frame in {@code argb} — or, while the ME lights are on, in the controller's colours running round it,
     * as {@link #title} runs them along a word. For the purple accent frames (the Home tab's head, the next level).
     */
    public static IGuiTexture border(int argb) {
        return new IGuiTexture() {
            @Override
            public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width,
                             float height, float partialTicks) {
                int left = Mth.floor(x), top = Mth.floor(y);
                int right = Mth.floor(x + width), bottom = Mth.floor(y + height);
                int w = right - left, h = bottom - top;
                if (w < 2 || h < 2) return;
                if (!active()) {
                    graphics.fill(left, top, right, top + 1, argb);
                    graphics.fill(left, bottom - 1, right, bottom, argb);
                    graphics.fill(left, top + 1, left + 1, bottom - 1, argb);
                    graphics.fill(right - 1, top + 1, right, bottom - 1, argb);
                    return;
                }
                float now = (System.currentTimeMillis() % (long) (FRAME_MS * FRAMES.length)) / FRAME_MS;
                // A whole cycle round the frame, so where it ends meets where it starts with no seam at the corner.
                final float LOOP = FRAMES.length;
                int perimeter = 2 * (w + h);
                int alpha = argb & 0xFF000000;
                // Clockwise from the top-left corner: top, right, bottom, left.
                for (int i = 0; i < w; i++) {
                    graphics.fill(left + i, top, left + i + 1, top + 1, alpha | colourAt(now - LOOP * i / perimeter));
                    int b = w + h + (w - 1 - i);
                    graphics.fill(left + i, bottom - 1, left + i + 1, bottom, alpha | colourAt(now - LOOP * b / perimeter));
                }
                for (int j = 1; j < h - 1; j++) {
                    int r = w + j;
                    graphics.fill(right - 1, top + j, right, top + j + 1, alpha | colourAt(now - LOOP * r / perimeter));
                    int l = 2 * w + h + (h - 1 - j);
                    graphics.fill(left, top + j, left + 1, top + j + 1, alpha | colourAt(now - LOOP * l / perimeter));
                }
            }
        };
    }

    /** The blended controller colour at a (fractional, wrapping) frame. */
    private static int colourAt(float frame) {
        float wrapped = ((frame % FRAMES.length) + FRAMES.length) % FRAMES.length;
        int a = (int) wrapped;
        return lerp(FRAMES[a], FRAMES[(a + 1) % FRAMES.length], wrapped - a);
    }

    private static int lerp(int a, int b, float f) {
        int r = (int) Mth.lerp(f, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(f, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(f, a & 0xFF, b & 0xFF);
        return (r << 16) | (g << 8) | bl;
    }
}
