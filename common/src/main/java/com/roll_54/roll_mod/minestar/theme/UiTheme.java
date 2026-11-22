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
    /**
     * LdLib's own Ore look.
     * @deprecated Merged into {@link #OREUI}: no longer offered, and a saved "ore" reads as green OreUI, its look ({@link #byId}).
     */
    @Deprecated
    ORE(StylesheetManager.ORE_MERGED),
    /** The old Ore look, as {@link #OREUI_GREEN} has it, in greys: the same LdLib Ore sheets and art, no green. */
    OREUI(StylesheetManager.ORE_MERGED),
    /** Bedrock's Ore UI at night: Dusk recoloured, with the OreUI pack's widgets darkened — see {@link ThemeSheets}. */
    OREUI_DARK(StylesheetManager.DUSK_MERGED),
    /** {@link #OREUI} in green: the old Ore look (LdLib's Ore sheets and art), which this theme took over. */
    OREUI_GREEN(StylesheetManager.ORE_MERGED),
    /**
     * Applied Energistics 2's screens: its lavender-grey panels, bevelled buttons and search field, drawn from AE2's own
     * GUI textures (the pack ships AE2), with the ME controller's flowing light along tab headers ({@link MeLights}).
     */
    AE2(StylesheetManager.DUSK_MERGED),
    GDP(StylesheetManager.GDP_MERGED);

    /** What a player who never opened the settings gets. */
    public static final UiTheme DEFAULT = GDP;

    private final List<ResourceLocation> sheets;

    /** Drawn from the OreUI pack's textures by {@link ThemeSheets} (now only the dark take). */
    public boolean oreUi() {
        return this == OREUI_DARK;
    }

    /** Whether its sheets are Dusk re-read with another palette ({@link ThemeSheets#sheet}). */
    public boolean recoloured() {
        return oreUi() || this == AE2;
    }

    /** Drawn with LdLib's own Ore sheets and art (the old Ore theme: OreUI and green OreUI). */
    public boolean oreArt() {
        return this == ORE || this == OREUI || this == OREUI_GREEN;
    }

    /** Whether Settings offers it: everything but the merged-away {@link #ORE}. */
    public boolean offered() {
        return this != ORE;
    }

    /** Whether the square/rounded corners setting changes anything: not on the pixel-art MC and Ore. */
    public boolean hasCorners() {
        // Every theme but vanilla's: AE2 and Ore round by their own cut-corner art (textures/gui/*_rounded).
        return this != MC;
    }

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
            // Text in vanilla's label grey rather than Ore's #222 — see assets/roll_mod/lss/ore.lss.
            case ORE -> new Palette(0xFF5A8A3C, 0xFF6B9E4A, 0xFF404040, 0xFF3A4A30, 0xFFF0E68C, 0xFF2E4A1E);
            // Ore's palette with its greens turned grey.
            case OREUI -> new Palette(0xFF7D7D7F, 0xFF8D8E90, 0xFF404040, 0xFF4A4B4F, 0xFFF0E68C, 0xFF3A3A3B);
            case OREUI_DARK -> new Palette(0xFF444445, 0xFF3A3A3B, 0xFFFFFFFF, 0xFFA0A1A4, 0xFF3C8527, 0xFF0C0C0C);
            case AE2 -> new Palette(0xFFCBCCD4, 0xFFE2E3E9, 0xFF413F54, 0xFF5D5B73, 0xFF3A8FC4, 0xFF413F54);
            case OREUI_GREEN -> ORE.palette();
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

    /** Each theme's recolour, built once ({@link #recolor} is read for every GUI blit). */
    private Recolor recolorCache;
    private boolean recolorBuilt;

    /** {@code null} for the vanilla-looking themes, which leave vanilla's art alone. */
    @javax.annotation.Nullable
    public Recolor recolor() {
        if (!recolorBuilt) {
            recolorCache = buildRecolor();
            recolorBuilt = true;
        }
        return recolorCache;
    }

    @javax.annotation.Nullable
    private Recolor buildRecolor() {
        return switch (this) {
            case DUSK -> new Recolor(0xFF0B0D11, 0xFF0B0D11, 0xFF0F1116, 0xFF171A21, 0xFF2B3242, 0xFFE7EAF2);
            case CARBON -> new Recolor(0xFF0A0A0A, 0xFF0A0A0A, 0xFF101010, 0xFF181818, 0xFF2E2E2E, 0xFFEDEDED);
            case MINT -> new Recolor(0xFF090D0C, 0xFF090D0C, 0xFF0D1210, 0xFF131A17, 0xFF26332E, 0xFFE4ECE8);
            case PLUM -> new Recolor(0xFF0D0A11, 0xFF0D0A11, 0xFF120F16, 0xFF1A1621, 0xFF31293C, 0xFFECE7F2);
            case PAPER -> new Recolor(0xFF9AA0AB, 0xFFB4BAC5, 0xFFDFE3EA, 0xFFEFF1F5, 0xFFFFFFFF, 0xFF1B1E24);
            case LATTE -> new Recolor(0xFFA1958A, 0xFFC4B8AA, 0xFFE8E0D5, 0xFFF5EFE6, 0xFFFFFCF7, 0xFF2B2622);
            case MODERN -> new Recolor(0xFF111114, 0xFF1E1F22, 0xFF1E1F22, 0xFF2C2C34, 0xFF3A3A44, 0xFFFFFFFF);
            // Sampled from LdLib's gdp_styles.png, so vanilla screens wear the same violet-grey as GDP's own panels and slots.
            case GDP -> new Recolor(0xFF0D0D11, 0xFF231C26, 0xFF2F2A34, 0xFF47434F, 0xFF85818D, 0xFFF3F2F2);
            case ORE -> new Recolor(0xFF1B2E12, 0xFF2E4A1E, 0xFF3F6A2A, 0xFF5A8A3C, 0xFF7AA85A, 0xFF404040);
            // Plain OreUI leaves vanilla's (and other mods') screens as they are, like the Minecraft theme: its look is
            // vanilla's own. Only the hub and LdLib's windows wear OreUI.
            case OREUI -> null;
            case OREUI_DARK -> new Recolor(0xFF0C0C0C, 0xFF121212, 0xFF19191A, 0xFF252526, 0xFF3A3A3B, 0xFFFFFFFF);
            case AE2 -> new Recolor(0xFF413F54, 0xFF878FA5, 0xFFADB0C4, 0xFFCBCCD4, 0xFFF2F2F2, 0xFF413F54);
            case OREUI_GREEN -> ORE.recolor();
            case MC -> null;
        };
    }

    /** {@link #DEFAULT} for anything unknown, so a hand-edited or outdated file never breaks a screen. */
    public static UiTheme byId(String id) {
        // Ore was merged into OreUI.
        if ("ore".equals(id)) return OREUI_GREEN;
        for (UiTheme theme : values()) {
            if (theme.id().equals(id)) return theme;
        }
        return DEFAULT;
    }
}
