package com.roll_54.roll_mod.earlywindow;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The colours of the player's hub theme, for the loading bars and panels. The theme is read straight
 * from {@code config/roll_mod/ui_theme.json} — roll_mod's own classes are not loaded yet — and the
 * palettes are a copy of {@code UiTheme.palette()} in :common; keep the two in step.
 *
 * @param panel  the panels behind text and bars (the theme's raised colour)
 * @param track  a bar's empty part (the theme's hovered colour)
 * @param accent a bar's filled part
 * @param line   the hairline around panels and bars
 * @param text   text on a panel
 */
record ThemeColours(int panel, int track, int accent, int line, int text) {

    private static final Pattern THEME = Pattern.compile("\"theme\"\\s*:\\s*\"([a-zA-Z_]+)\"");

    static ThemeColours load() {
        String id = "gdp"; // UiTheme.DEFAULT
        try {
            Path file = FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("ui_theme.json");
            if (Files.isRegularFile(file)) {
                Matcher m = THEME.matcher(Files.readString(file));
                if (m.find()) id = m.group(1).toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
            // No preference yet, or unreadable: the default theme.
        }
        return of(id);
    }

    static ThemeColours of(String id) {
        return switch (id) {
            case "dusk" -> new ThemeColours(0xFF212632, 0xFF2B3242, 0xFF7C8CFF, 0x16FFFFFF, 0xFFE7EAF2);
            case "carbon" -> new ThemeColours(0xFF232323, 0xFF2E2E2E, 0xFFF0A04B, 0x16FFFFFF, 0xFFEDEDED);
            case "mint" -> new ThemeColours(0xFF1D2723, 0xFF26332E, 0xFF4ADE80, 0x16FFFFFF, 0xFFE4ECE8);
            case "plum" -> new ThemeColours(0xFF251F2E, 0xFF31293C, 0xFFE879F9, 0x16FFFFFF, 0xFFECE7F2);
            case "paper" -> new ThemeColours(0xFFFFFFFF, 0xFFEEF1F6, 0xFF3B6EF6, 0x1F000000, 0xFF1B1E24);
            case "latte" -> new ThemeColours(0xFFFFFCF7, 0xFFEFE7DB, 0xFFC2703F, 0x1F000000, 0xFF2B2622);
            case "modern" -> new ThemeColours(0xFF2C2C34, 0xFF3A3A44, 0xFF71A4F4, 0xFF7F8084, 0xFFFFFFFF);
            case "ore" -> new ThemeColours(0xFF5A8A3C, 0xFF6B9E4A, 0xFFF0E68C, 0xFF2E4A1E, 0xFF404040);
            // The vanilla-looking theme, which has no palette: vanilla's loading bar, white on dark.
            case "mc" -> new ThemeColours(0xFF1E1E1E, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
            default -> new ThemeColours(0xFF3A3A3A, 0xFF4A4A4A, 0xFF5AA0E6, 0xFF1E1E1E, 0xFFE0E0E0); // gdp
        };
    }
}
