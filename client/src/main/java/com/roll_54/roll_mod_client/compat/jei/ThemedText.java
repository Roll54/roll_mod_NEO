package com.roll_54.roll_mod_client.compat.jei;

import com.roll_54.roll_mod.minestar.theme.GuiRecolor;

/**
 * Text colours for the recipe pages roll_mod draws itself. Their colours were picked for the vanilla light-grey panel;
 * on a dark theme that panel is dark, and dark grey or deep colours vanish into it. There the muted lines take
 * {@link #MUTED_ON_DARK} and each coloured line its light variant; on vanilla or a light theme they stay as they were.
 */
public final class ThemedText {

    /** Muted text on the hub's dark panels (CLAUDE.md: 0xD8D8D8 or lighter, never grey on grey). */
    public static final int MUTED_ON_DARK = 0xFFD8D8D8;
    private static final int VANILLA_LABEL = 0x404040;

    private ThemedText() {}

    /** Whether the active theme draws panels dark: its text colour is light. */
    public static boolean darkPanel() {
        int text = GuiRecolor.labelColor(VANILLA_LABEL) & 0xFFFFFF;
        if (text == VANILLA_LABEL) return false;
        int r = (text >> 16) & 0xFF, g = (text >> 8) & 0xFF, b = text & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > 0x80;
    }

    /** {@code dark} on a light panel, {@code light} on a dark one. */
    public static int accent(int dark, int light) {
        return darkPanel() ? light : dark;
    }

    /** A muted line: vanilla's {@code vanilla} grey, the theme's text on a light theme, light grey on a dark one. */
    public static int muted(int vanilla) {
        if (darkPanel()) return MUTED_ON_DARK;
        return 0xFF000000 | GuiRecolor.labelColor(vanilla & 0xFFFFFF);
    }
}
