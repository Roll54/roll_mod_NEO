package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * A bevelled box in the shape of AE2's buttons, in any colours: a 1px outline, a 1px light edge along the top and left
 * inside it, a 1px shade along the bottom and right, and the face. The hub's bookmarks use it on every theme, so the
 * tab strip is the same shape whichever theme is picked; only the colours follow the theme ({@link HubTabBox}).
 * Snapped to whole pixels, so the outline never turns into a light edge between pixels.
 */
final class HubBevel implements IGuiTexture {

    private final int face;
    private final int light;
    private final int shade;
    private final int outline;

    HubBevel(int face, int light, int shade, int outline) {
        this.face = face;
        this.light = light;
        this.shade = shade;
        this.outline = outline;
    }

    /** A bevel worked out from one face colour: lighter and darker steps of it, and a dark outline. */
    static HubBevel of(int face, int outline) {
        return new HubBevel(face, scale(face, 1.25f), scale(face, 0.72f), outline);
    }

    @Override
    public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height,
                     float partialTicks) {
        int l = Math.round(x), t = Math.round(y), r = Math.round(x + width), b = Math.round(y + height);
        if (r - l < 4 || b - t < 4) return;
        // Rounded corners (Settings): the outline's four corner pixels left out, the 1px cut the themes' art uses.
        int c = com.roll_54.roll_mod.minestar.theme.UiThemeState.get().hasCorners()
                && !com.roll_54.roll_mod.minestar.theme.UiThemeState.squareCorners() ? 1 : 0;
        graphics.fill(l + c, t, r - c, t + 1, outline);
        graphics.fill(l + c, b - 1, r - c, b, outline);
        graphics.fill(l, t + c, l + 1, b - c, outline);
        graphics.fill(r - 1, t + c, r, b - c, outline);
        graphics.fill(l + 1, t + 1, r - 1, b - 1, face);
        graphics.fill(l + 1, t + 1, r - 1, t + 2, light);
        graphics.fill(l + 1, t + 1, l + 2, b - 1, light);
        graphics.fill(l + 1, b - 2, r - 1, b - 1, shade);
        graphics.fill(r - 2, t + 1, r - 1, b - 1, shade);
        if (c == 1) {
            // The cut's diagonal: the inner corner pixels take the outline, as a pixel-art rounded box has it.
            graphics.fill(l + 1, t + 1, l + 2, t + 2, outline);
            graphics.fill(r - 2, t + 1, r - 1, t + 2, outline);
            graphics.fill(l + 1, b - 2, l + 2, b - 1, outline);
            graphics.fill(r - 2, b - 2, r - 1, b - 1, outline);
        }
    }

    /** {@code argb} with its colour multiplied by {@code k} (clamped), alpha kept opaque. */
    static int scale(int argb, float k) {
        int rr = Mth.clamp(Math.round(((argb >> 16) & 0xFF) * k), 0, 255);
        int gg = Mth.clamp(Math.round(((argb >> 8) & 0xFF) * k), 0, 255);
        int bb = Mth.clamp(Math.round((argb & 0xFF) * k), 0, 255);
        // A face that is almost black would not lighten by multiplying; give it a floor to lighten from.
        if (k > 1 && rr + gg + bb < 60) {
            rr = Math.min(255, rr + 28);
            gg = Math.min(255, gg + 28);
            bb = Math.min(255, bb + 28);
        }
        return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
    }
}
