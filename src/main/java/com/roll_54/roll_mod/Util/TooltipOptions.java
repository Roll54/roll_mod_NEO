package com.roll_54.roll_mod.Util;

public record TooltipOptions(int loreLines, Integer nameColorHex, Integer loreColorHex, boolean glow) {
    public static final TooltipOptions NONE = new TooltipOptions(0, null, null, false);

    public TooltipOptions {
        if (loreLines < 0) throw new IllegalArgumentException("loreLines must be >= 0");
    }

    public boolean hasNameColor() { return nameColorHex != null; }
    public boolean hasLoreColor() { return loreColorHex != null; }
    public boolean hasLore() { return loreLines > 0; }

    // --- factory helpers ---
    /** custom color*/
    public static TooltipOptions name(int nameHex) {
        return new TooltipOptions(0, nameHex, null, false);
    }
    /** custom Name color + glow */
    public static TooltipOptions nameAndGlow(int nameHex) {
        return new TooltipOptions(0, nameHex, null, true);
    }

    /** custom lore*/
    public static TooltipOptions lore(int lines, int loreHex) {
        return new TooltipOptions(lines, null, loreHex, false);
    }

    /** custom color + custom lore color */
    public static TooltipOptions nameAndLore(int nameHex, int lines, int loreHex) {
        return new TooltipOptions(lines, nameHex, loreHex, false);
    }

    /** everything */
    public static TooltipOptions nameLoreGlow(int nameHex, int lines, int loreHex) {
        return new TooltipOptions(lines, nameHex, loreHex, true);
    }
}
