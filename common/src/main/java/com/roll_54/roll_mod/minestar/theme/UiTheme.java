package com.roll_54.roll_mod.minestar.theme;

import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

/**
 * The look of every LdLib screen the mod opens — LdLib2's own built-in themes, picked by the player
 * in the hub's settings. See {@link RollUi}.
 *
 * <p>Every built-in sheet LdLib ships is offered. {@link #MC} is the vanilla panel look every block
 * screen used before themes existed, so it keeps the exact pair those screens passed.
 */
public enum UiTheme {
    MODERN(StylesheetManager.MODERN_MERGED),
    DUSK(StylesheetManager.DUSK_MERGED),
    CARBON(StylesheetManager.CARBON_MERGED),
    MINT(StylesheetManager.MINT_MERGED),
    PLUM(StylesheetManager.PLUM_MERGED),
    PAPER(StylesheetManager.PAPER_MERGED),
    LATTE(StylesheetManager.LATTE_MERGED),
    MC(StylesheetManager.MC_MERGED, StylesheetManager.GDP_MERGED),
    ORE(StylesheetManager.ORE_MERGED),
    GDP(StylesheetManager.GDP_MERGED);

    /** What a player who never opened the settings gets. */
    public static final UiTheme DEFAULT = GDP;

    private final List<ResourceLocation> sheets;

    UiTheme(ResourceLocation... sheets) {
        this.sheets = List.of(sheets);
    }

    public List<ResourceLocation> sheets() {
        return sheets;
    }

    /** The name as stored in the preference file. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("gui." + RollMod.MODID + ".theme." + id());
    }

    /**
     * The colours a widget drawn outside LdLib — the chat buttons — uses to match this theme, from
     * the palette each theme's {@code .lss} file declares in its header. ARGB.
     *
     * @param raised  a control at rest
     * @param hovered a control under the cursor
     * @param text    normal text
     * @param dim     secondary text
     * @param accent  the theme's one accent: selection, focus
     * @param line    the 1px hairline around controls
     */
    public record Palette(int raised, int hovered, int text, int dim, int accent, int line) {}

    /** {@code null} for the vanilla-looking themes, whose widgets should simply stay vanilla. */
    @javax.annotation.Nullable
    public Palette palette() {
        return switch (this) {
            case DUSK -> new Palette(0xFF212632, 0xFF2B3242, 0xFFE7EAF2, 0xFF98A0B4, 0xFF7C8CFF, 0x16FFFFFF);
            case CARBON -> new Palette(0xFF232323, 0xFF2E2E2E, 0xFFEDEDED, 0xFF9A9A9A, 0xFFF0A04B, 0x16FFFFFF);
            case MINT -> new Palette(0xFF1D2723, 0xFF26332E, 0xFFE4ECE8, 0xFF94A49C, 0xFF4ADE80, 0x16FFFFFF);
            case PLUM -> new Palette(0xFF251F2E, 0xFF31293C, 0xFFECE7F2, 0xFFA89AB4, 0xFFE879F9, 0x16FFFFFF);
            case PAPER -> new Palette(0xFFFFFFFF, 0xFFEEF1F6, 0xFF1B1E24, 0xFF5B6270, 0xFF3B6EF6, 0x1F000000);
            case LATTE -> new Palette(0xFFFFFCF7, 0xFFEFE7DB, 0xFF2B2622, 0xFF6B6055, 0xFFC2703F, 0x1F000000);
            case MODERN -> new Palette(0xFF2C2C34, 0xFF3A3A44, 0xFFFFFFFF, 0xFF9A9AA4, 0xFF71A4F4, 0xFF7F8084);
            case GDP -> new Palette(0xFF3A3A3A, 0xFF4A4A4A, 0xFFE0E0E0, 0xFF9A9A9A, 0xFF5AA0E6, 0xFF1E1E1E);
            case ORE -> new Palette(0xFF5A8A3C, 0xFF6B9E4A, 0xFF222222, 0xFF3A4A30, 0xFFF0E68C, 0xFF2E4A1E);
            case MC -> null;
        };
    }

    /**
     * What vanilla's grey GUI art is recoloured to when "Use the hub theme everywhere" is on — see
     * {@code GuiRecolor}. Vanilla draws its screens from a handful of greys; each one is mapped to a
     * tone of this theme, and the greys between them are blended. ARGB.
     *
     * @param outline   for vanilla's black outlines (0%)
     * @param shadow    for the dark bevel, {@code #555555} (33%)
     * @param well      for slots and sunken areas, {@code #8B8B8B} (55%)
     * @param surface   for the panel face, {@code #C6C6C6} (78%)
     * @param highlight for the light bevel, white (100%)
     * @param text      for the labels vanilla writes in dark grey ("Inventory", a chest's name)
     */
    public record Recolor(int outline, int shadow, int well, int surface, int highlight, int text) {}

    /** {@code null} for the vanilla-looking themes, which leave vanilla's art alone. */
    @javax.annotation.Nullable
    public Recolor recolor() {
        return switch (this) {
            case DUSK -> new Recolor(0xFF0B0D11, 0xFF0B0D11, 0xFF0F1116, 0xFF171A21, 0xFF2B3242, 0xFFE7EAF2);
            case CARBON -> new Recolor(0xFF0A0A0A, 0xFF0A0A0A, 0xFF101010, 0xFF181818, 0xFF2E2E2E, 0xFFEDEDED);
            case MINT -> new Recolor(0xFF090D0C, 0xFF090D0C, 0xFF0D1210, 0xFF131A17, 0xFF26332E, 0xFFE4ECE8);
            case PLUM -> new Recolor(0xFF0D0A11, 0xFF0D0A11, 0xFF120F16, 0xFF1A1621, 0xFF31293C, 0xFFECE7F2);
            case PAPER -> new Recolor(0xFF9AA0AB, 0xFFB4BAC5, 0xFFDFE3EA, 0xFFEFF1F5, 0xFFFFFFFF, 0xFF1B1E24);
            case LATTE -> new Recolor(0xFFA1958A, 0xFFC4B8AA, 0xFFE8E0D5, 0xFFF5EFE6, 0xFFFFFCF7, 0xFF2B2622);
            case MODERN -> new Recolor(0xFF111114, 0xFF1E1F22, 0xFF1E1F22, 0xFF2C2C34, 0xFF3A3A44, 0xFFFFFFFF);
            case GDP -> new Recolor(0xFF111111, 0xFF1E1E1E, 0xFF262626, 0xFF3A3A3A, 0xFF4A4A4A, 0xFFE0E0E0);
            case ORE -> new Recolor(0xFF1B2E12, 0xFF2E4A1E, 0xFF3F6A2A, 0xFF5A8A3C, 0xFF7AA85A, 0xFF1A1A1A);
            case MC -> null;
        };
    }

    /** {@link #DEFAULT} for anything unknown, so a hand-edited or outdated file never breaks a screen. */
    public static UiTheme byId(String id) {
        for (UiTheme theme : values()) {
            if (theme.id().equals(id)) return theme;
        }
        return DEFAULT;
    }
}
