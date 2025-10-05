package com.roll_54.roll_mod.Util;

public record TooltipOptions(int loreLines, Integer nameColorHex, Integer loreColorHex) {
    public static final TooltipOptions NONE = new TooltipOptions(0, null, null);

    public TooltipOptions {
        if (loreLines < 0) throw new IllegalArgumentException("loreLines must be >= 0");
    }

    public boolean hasNameColor() { return nameColorHex != null; }
    public boolean hasLoreColor() { return loreColorHex != null; }
    public boolean hasLore() { return loreLines > 0; }

    // --- factory helpers ---
    /** Тільки колір назви */
    public static TooltipOptions name(int nameHex) {
        return new TooltipOptions(0, nameHex, null);
    }

    /** Тільки лор: N рядків із кольором лору */
    public static TooltipOptions lore(int lines, int loreHex) {
        return new TooltipOptions(lines, null, loreHex);
    }

    /** І колір назви, і лор: N рядків з кольором лору */
    public static TooltipOptions nameAndLore(int nameHex, int lines, int loreHex) {
        return new TooltipOptions(lines, nameHex, loreHex);
    }
}
