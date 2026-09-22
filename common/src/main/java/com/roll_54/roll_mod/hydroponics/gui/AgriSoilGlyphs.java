package com.roll_54.roll_mod.hydroponics.gui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * AgriCraft's own humidity / acidity / nutrient glyphs, as LdLib2 textures.
 *
 * <p>Reusing AgriCraft's art means the bed's sliders read exactly like the crop requirement pages
 * players already know, and that this mod ships no icon textures of its own.
 *
 * <p>The sprites live in AgriCraft's {@code gui_components.png} as three rows of variable-width,
 * 12 px tall glyphs. The widths are AgriCraft's own {@code CropRequirementCategory.*_OFFSETS}
 * arrays; a glyph's U is the running sum of the widths before it, which is how AgriCraft itself
 * computes it.
 *
 * <p>Unlike the {@code GuiGraphics.blit} this replaces, {@link SpriteTexture} resolves the sheet's
 * dimensions from the texture manager on first draw, so the sheet's size is not written down here
 * and cannot go stale if AgriCraft grows it.
 */
public final class AgriSoilGlyphs {

    public static final ResourceLocation GUI_COMPONENTS =
            ResourceLocation.fromNamespaceAndPath("agricraft", "textures/gui/gui_components.png");

    /** Every glyph is this tall; the widths vary per value. */
    public static final int HEIGHT = 12;

    /** A row of the sprite sheet: one soil property, one glyph per value. */
    public enum Row {
        HUMIDITY(0, new int[]{8, 8, 10, 10, 10, 7}),
        ACIDITY(12, new int[]{7, 8, 7, 8, 8, 8, 6}),
        NUTRIENTS(24, new int[]{6, 8, 9, 9, 11, 10});

        private final int v;
        private final int[] widths;

        Row(int v, int[] widths) {
            this.v = v;
            this.widths = widths;
        }

        private int u(int ordinal) {
            int u = 0;
            for (int i = 0; i < ordinal; i++) {
                u += widths[i];
            }
            return u;
        }

        public int width(int ordinal) {
            return widths[clamp(ordinal)];
        }

        private int clamp(int ordinal) {
            return Math.clamp(ordinal, 0, widths.length - 1);
        }
    }

    private AgriSoilGlyphs() {
    }

    /**
     * The glyph for {@code ordinal} (0-based, i.e. the setting value minus one).
     *
     * <p>Built fresh on every call rather than cached in a table: a {@link SpriteTexture} is mutable
     * and an element that is handed one owns it, so sharing instances across elements would make a
     * later {@code setColor} on one of them bleed into the others.
     */
    public static IGuiTexture glyph(Row row, int ordinal) {
        int safe = row.clamp(ordinal);
        return SpriteTexture.of(GUI_COMPONENTS)
                .setSprite(row.u(safe), row.v, row.width(safe), HEIGHT);
    }
}
