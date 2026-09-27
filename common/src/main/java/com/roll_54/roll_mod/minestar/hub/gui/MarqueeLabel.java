package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.function.BooleanSupplier;

/**
 * One line of text, clipped to its box, that scrolls sideways while {@code hovered} holds and the
 * text is longer than {@link #MIN_CHARS} characters — so a long letter title can be read in a list
 * row that only has room for the start of it.
 *
 * <p>The scroll runs to the end of the text, rests there, then starts over from the beginning, and
 * snaps back the moment the cursor leaves. {@code hovered} is supplied rather than read from this
 * element because the row, not the text, is what takes the mouse: the text sits in it with hit
 * testing off so a click lands on the row.
 *
 * <p>Built on the dedicated server too, since the hub's tree is built identically on both sides;
 * the font and the clock live in {@link ClientRender}, which the dist check never reaches there —
 * the {@link PlayerHeadElement} shape.
 */
public class MarqueeLabel extends UIElement {

    /** Titles this short never scroll, even when a wide font would cut them. */
    public static final int MIN_CHARS = 15;

    /** How fast the text moves, in pixels per second. */
    private static final float SPEED = 20f;
    /** How long it rests at each end before moving on, in milliseconds. */
    private static final long PAUSE = 1000L;

    private final Component text;
    private final int plainLength;
    private final BooleanSupplier hovered;
    private final int color;

    /** When the current hover began, or {@code -1} while not hovered. */
    private long hoverStart = -1L;

    public MarqueeLabel(Component text, BooleanSupplier hovered, int color) {
        this.text = text;
        this.plainLength = text.getString().length();
        this.hovered = hovered;
        this.color = color;
        setAllowHitTest(false);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        ClientRender.draw(this, context);
    }

    /** Isolated holder for the client-only font and clock. */
    private static final class ClientRender {

        static void draw(MarqueeLabel label, GUIContext context) {
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            float x = label.getContentX();
            float y = label.getContentY();
            float w = label.getContentWidth();
            float h = label.getContentHeight();
            int overflow = Math.max(0, font.width(label.text) - (int) Math.floor(w));

            float offset = 0f;
            boolean scrolls = overflow > 0 && label.plainLength > MIN_CHARS
                    && label.hovered.getAsBoolean();
            if (scrolls) {
                long now = net.minecraft.Util.getMillis();
                if (label.hoverStart < 0L) label.hoverStart = now;
                offset = offsetAt(now - label.hoverStart, overflow);
            } else {
                label.hoverStart = -1L;
            }

            context.enableScissor(x, y, w, h);
            context.pose.pushPose();
            context.pose.translate(x - offset, y + (h - font.lineHeight) / 2f + 1f, 0);
            context.graphics.drawString(font, label.text, 0, 0, label.color, false);
            context.pose.popPose();
            context.disableScissor();
        }

        /** Rest at the start, move to the end, rest there, and loop. */
        private static float offsetAt(long elapsed, int overflow) {
            long travel = (long) (overflow / SPEED * 1000f);
            long cycle = PAUSE + travel + PAUSE;
            long t = elapsed % cycle;
            if (t < PAUSE) return 0f;
            if (t < PAUSE + travel) return (t - PAUSE) / 1000f * SPEED;
            return overflow;
        }
    }
}
