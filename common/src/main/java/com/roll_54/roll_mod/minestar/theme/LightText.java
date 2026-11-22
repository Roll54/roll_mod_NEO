package com.roll_54.roll_mod.minestar.theme;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

import java.util.function.Supplier;

/**
 * Text on the light themes (Paper, Latte, Applied Energistics). The mod's screens colour much of their text in code,
 * picked for dark panels — white names, {@code ChatFormatting.GRAY} hints, yellow values — and on a light panel those
 * all but vanish. While a light theme is on, every LdLib text draw ({@code LdLibTextColorMixin}) passes through here:
 * white and near-white become the theme's text colour, greys its dim colour, and other bright colours are darkened
 * until they read; drop shadows are dropped. Dark themes are left exactly as they were.
 */
public final class LightText {

    private LightText() {}

    /** Whether the current theme is a light one. Client only. */
    public static boolean active() {
        UiTheme theme = UiThemeState.get();
        return theme == UiTheme.PAPER || theme == UiTheme.LATTE || theme == UiTheme.AE2;
    }

    /** Marks text whose colours are meant to glow (the AE2 ME titles): darkened far less than ordinary text. */
    public static final String GLOW = "roll_mod:glow";

    /** A colour as it should read on the current light theme's panels. Alpha kept. */
    public static int color(int argb) {
        return color(argb, 0.35f);
    }

    /** As {@link #color(int)}, with bright colours brought down only to {@code ceiling} lightness. */
    public static int color(int argb, float ceiling) {
        UiTheme.Palette palette = UiThemeState.get().palette();
        if (palette == null) return argb;
        int alpha = argb & 0xFF000000;
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float light = (max + min) / 510f;
        boolean grey = max - min < 24;
        if (grey) {
            if (light > 0.85f) return alpha | (palette.text() & 0xFFFFFF);
            if (light > 0.4f) return alpha | (palette.dim() & 0xFFFFFF);
            return argb;
        }
        // A bright colour (yellow, aqua, light green): darkened to about a third of full, keeping its hue — half was
        // still too pale to read on AE2's lavender (aqua came out #3fbfbf).
        if (light > ceiling) {
            float k = ceiling / light;
            return alpha | ((int) (r * k) << 16) | ((int) (g * k) << 8) | (int) (b * k);
        }
        return argb;
    }

    private static Style style(Style style) {
        TextColor color = style.getColor();
        if (color == null) return style;
        // The ME titles keep most of their brightness: only enough taken off to read on a light panel.
        float ceiling = GLOW.equals(style.getInsertion()) ? 0.58f : 0.35f;
        int mapped = color(0xFF000000 | color.getValue(), ceiling) & 0xFFFFFF;
        return mapped == color.getValue() ? style : style.withColor(mapped);
    }

    private static final java.util.Map<IconKey, com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture> ICONS = new java.util.HashMap<>();
    private record IconKey(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture icon, int color) {}

    /** A white icon as it should show now: as it is on dark themes, in the theme's text colour on light ones. Cached. */
    public static com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture icon(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture icon) {
        if (!active()) return icon;
        UiTheme.Palette palette = UiThemeState.get().palette();
        if (palette == null) return icon;
        int color = 0xFF000000 | palette.text();
        return ICONS.computeIfAbsent(new IconKey(icon, color), k -> k.icon().copy().setColor(k.color()));
    }

    /**
     * A flat colour the hub fills a panel with, as it should show now. The hub darkens panels with translucent black
     * (rows, side panels, wells), which on the Applied Energistics theme turned its lavender into plain grey; there
     * the black becomes AE2's own deep blue-violet, so a darker panel is a darker AE2 shade. Everything else unchanged.
     */
    public static int fill(int argb) {
        if ((argb & 0xFFFFFF) != 0 || (argb >>> 24) == 0xFF) return argb;
        if (UiThemeState.get() != UiTheme.AE2) return argb;
        return (argb & 0xFF000000) | 0x3A3F6E;
    }

    /** The text with every styled colour passed through {@link #color}. */
    public static FormattedCharSequence sequence(FormattedCharSequence text) {
        return sink -> text.accept((index, style, codePoint) -> sink.accept(index, style(style), codePoint));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Supplier wrap(Supplier supplier) {
        return () -> {
            Object text = supplier.get();
            return text instanceof FormattedCharSequence seq ? sequence(seq) : text;
        };
    }

    /**
     * The key LdLib caches a text's layout (glyphs and their colours) under: the text plus the theme, so a theme change
     * lays the text out again instead of reusing the other theme's colours.
     */
    public record Key(Object text, UiTheme theme) {}
}
