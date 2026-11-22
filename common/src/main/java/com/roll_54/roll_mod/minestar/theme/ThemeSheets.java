package com.roll_54.roll_mod.minestar.theme;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The stylesheets {@link RollUi} needs beyond LdLib's own: the square-cornered take on a theme, and
 * Ore UI ({@link UiTheme#OREUI_DARK}). Client only.
 *
 * <p><b>Corners.</b> LdLib's sdf themes (Modern, Dusk, Carbon, Mint, Plum, Paper, Latte) draw every
 * surface as {@code sdf(colour, radius, …)}. The square take re-reads the very files LdLib merged the
 * theme from — every mod's {@code lss/<theme>.lss}, LdLib's first — with each radius set to 0. GDP
 * draws from its own rounded sprites in code, so square GDP is a short sheet laid over it that swaps
 * each {@code RECT_RD*} for the matching {@code RECT*}. MC and Ore are pixel art with no radius.
 *
 * <p><b>Ore UI.</b> Bedrock's Ore UI, from the OreUI Recreation pack by tmc249 (MIT, see
 * {@code assets/ldlib2/textures/gui/oreui/LICENSE.txt}): Dusk — the fullest sdf theme, so every component is
 * covered — with its palette swapped for Ore UI's greys and green, under a sheet that draws the
 * buttons, fields, checkboxes, sliders, tabs and scrollbars from the pack's own textures. Rounded
 * uses {@code assets/ldlib2/textures/gui/oreui/rounded/} (the pack's art with the corner pixels cut) and small radii;
 * square uses the pack's art as it is.
 *
 * <p>Built on first use and kept until resources reload (F3+T, a resource pack change).
 */
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class ThemeSheets {

    private ThemeSheets() {}

    /** Built sheets, by "merged id|square|oreui". */
    private static final Map<String, Stylesheet> CACHE = new HashMap<>();
    /** By "square|dark". */
    private static final Map<String, Stylesheet> OREUI_WIDGETS = new HashMap<>();
    /** What OreUI dark multiplies the pack's grey widgets by: #7d7d7f buttons come out near #434344. */
    public static final String DARK_TINT = "8a8a8a";

    /** The tint a theme's grey OreUI widgets are drawn with, or null for the pack's own grey. */
    @javax.annotation.Nullable
    public static String tint(UiTheme theme) {
        return switch (theme) {
            case OREUI_DARK -> DARK_TINT;
            default -> null;
        };
    }
    private static Stylesheet gdpSquare;
    /**
     * Bumped whenever a sheet here is parsed anew or resources reload. LdLib settles two rules of equal specificity
     * by which was <i>created</i> last (a global counter in {@code StyleRule}), not by which sheet comes last in the
     * list — so a theme sheet parsed here after {@link RollUi}'s geometry or {@link UiFont}'s sheet silently beat
     * them (buttons back at the theme's 16px, labels back in its font). Every sheet laid over a theme rebuilds
     * itself when this moves, so it is always newer than the theme it overrides.
     */
    private static int generation;

    /** See {@link #generation}. */
    public static synchronized int generation() {
        return generation;
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> clear());
    }

    private static synchronized void clear() {
        CACHE.clear();
        OREUI_WIDGETS.clear();
        AE2_WIDGETS.clear();
        gdpSquare = null;
        GDP_SLOTS[0] = GDP_SLOTS[1] = null;
        oreRounded = null;
        generation++;
    }

    /**
     * {@code mergedId} (one of {@link StylesheetManager}'s {@code *_MERGED}) as {@code theme} wants it:
     * LdLib's own when there is nothing to change, else re-read and rewritten.
     */
    public static synchronized Stylesheet sheet(ResourceLocation mergedId, UiTheme theme, boolean square) {
        boolean oreui = theme.recoloured();
        if (!square && !oreui) return StylesheetManager.INSTANCE.getStylesheetSafe(mergedId);
        String key = mergedId + "|" + square + "|" + theme;
        Stylesheet cached = CACHE.get(key);
        if (cached != null) return cached;

        String text = read(mergedId);
        if (text == null) return StylesheetManager.INSTANCE.getStylesheetSafe(mergedId);
        if (oreui) text = orePalette(text, theme);
        // Ore UI is pixel art: when rounded, only a pixel or two of rounding, never Dusk's soft 5-7.
        IntUnaryOperator radius = square ? r -> 0 : oreui ? r -> Math.min(r, 2) : r -> r;
        Stylesheet sheet = Stylesheet.parse(radii(text, radius));
        CACHE.put(key, sheet);
        // Everything laid over a theme must be parsed after it — see generation.
        OREUI_WIDGETS.clear();
        AE2_WIDGETS.clear();
        gdpSquare = null;
        oreRounded = null;
        generation++;
        return sheet;
    }

    /** Ore UI's widgets drawn from the pack's textures, laid over its recoloured Dusk. */
    public static synchronized Stylesheet oreUiWidgets(boolean square, UiTheme theme) {
        return OREUI_WIDGETS.computeIfAbsent(square + "|" + theme, k -> Stylesheet.parse(oreUiWidgetsText(square, theme)));
    }

    private static final Map<Boolean, Stylesheet> AE2_WIDGETS = new HashMap<>();

    /** AE2's widgets from its own GUI textures, laid over its recoloured Dusk. AE2 is pixel art: square either way. */
    public static synchronized Stylesheet ae2Widgets(boolean square) {
        return AE2_WIDGETS.computeIfAbsent(square, k -> Stylesheet.parse(ae2Corners(square, """
                button:host,
                tag-field:host button,
                color-selector:host button,
                selector:host {
                  base-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4);
                  hover-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4) color(#dff3ff);
                  pressed-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4) color(#b4b8cc);
                }
                selector:host,
                selector:host:hovered {
                  background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4);
                }
                button:disabled:host {
                  base-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4) color(#b4b4bc);
                }
                button:host .__button_text__,
                .__selector_preview__ label {
                  text-color: #413f54;
                  text-shadow: false;
                }
                button:disabled:host .__button_text__ {
                  text-color: #878fa5;
                }
                .__confirm-button__ {
                  base-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4) color(#a8f0b0);
                }
                .__reject-button__ {
                  base-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4) color(#ffb0a8);
                }
                item-slot,
                .item-slot_bg,
                fluid-slot,
                .fluid-slot_bg {
                  background: sprite(ae2:textures/guis/interface.png, 26, 71, 16, 16) scale(0.8889);
                }
                text-field:host,
                search-component:host,
                color-selector:host text-field,
                .__tag-field_text-field__,
                .__search-component_text-field__,
                .__text-area_content-view__ {
                  background: sprite(ae2:textures/guis/text_field.png, 0, 0, 128, 12, 1, 3, 1, 1);
                  padding-top: 4;
                  padding-bottom: 2;
                }
                text-field:host,
                search-component:host,
                text-area:host,
                color-selector:host text-field {
                  focus-overlay: sdf(#00000000, 0, 1, #3a8fc4);
                }
                .panel_bg {
                  background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 4, 4, 4, 4);
                }
                tab:host {
                  base-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4);
                  hover-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4) color(#dff3ff);
                  pressed-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4) color(#b4b8cc);
                }
                tab:host:selected {
                  base-background: sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4) color(#b4b8cc);
                }
                scroller-horizontal .__scroller_scroll_bar__,
                scroller-vertical .__scroller_scroll_bar__ {
                  base-background: sdf(#cbccd4, 0, 1, #413f54);
                  hover-background: sdf(#e2e3e9, 0, 1, #413f54);
                  pressed-background: sdf(#adb0c4, 0, 1, #413f54);
                }
                .__scroller_scroll_container__ {
                  background: sdf(#9a9fb4, 0, 1, #878fa5);
                }
                .__progress-bar_bar__ {
                  background: sdf(#3a8fc4, 0);
                }
                """)));
    }

    /**
     * AE2's buttons with their corners cut (our own bevel in AE2's colours, textures/gui/ae2_rounded) when the player
     * wants rounded corners; AE2's own square art otherwise.
     */
    private static String ae2Corners(boolean square, String text) {
        if (square) return text;
        String bevel = "sprite(ae2:textures/guis/background.png, 0, 0, 256, 256, 2, 2, 2, 4)";
        return text
                .replace(bevel + " color(#b4b8cc)", "sprite(ldlib2:textures/gui/ae2_rounded/button_pressed.png, 0, 0, 8, 9, 2, 2, 2, 4)")
                .replace(bevel, "sprite(ldlib2:textures/gui/ae2_rounded/button.png, 0, 0, 8, 9, 2, 2, 2, 4)");
    }

    private static Stylesheet oreRounded;

    /**
     * The Ore themes (OreUI, green OreUI) with rounded corners: LdLib's Ore buttons, fields and wells from a copy of
     * its atlas with each piece's corner pixels cut (textures/gui/ore_rounded/ore_styles.png), laid over Ore's sheet.
     */
    public static synchronized Stylesheet oreRounded() {
        if (oreRounded == null) {
            String a = "ldlib2:textures/gui/ore_rounded/ore_styles.png";
            String btn = "sprite(" + a + ", 0, 0, 5, 7, 2, 2, 2, 4)";
            String pressed = "sprite(" + a + ", 5, 0, 5, 7, 2, 2, 2, 4)";
            String off = "sprite(" + a + ", 10, 0, 5, 7, 2, 2, 2, 4)";
            String green = "sprite(" + a + ", 26, 0, 5, 7, 2, 2, 2, 4)";
            String greenDown = "sprite(" + a + ", 31, 0, 5, 7, 2, 2, 2, 4)";
            String red = "sprite(" + a + ", 36, 0, 5, 7, 2, 2, 2, 4)";
            String redDown = "sprite(" + a + ", 41, 0, 5, 7, 2, 2, 2, 4)";
            String small = "sprite(" + a + ", 46, 0, 5, 5, 2, 2, 2, 2)";
            String smallHover = "sprite(" + a + ", 51, 0, 5, 5, 2, 2, 2, 2)";
            String smallDown = "sprite(" + a + ", 56, 0, 5, 5, 2, 2, 2, 2)";
            String well = "sprite(" + a + ", 21, 0, 3, 4, 1, 1, 1, 2)";
            String field = "sprite(" + a + ", 24, 0, 3, 5, 1, 3, 1, 1)";
            oreRounded = Stylesheet.parse("""
                    button:host,
                    tag-field:host button {
                      base-background: %1$s;
                      hover-background: %2$s;
                      pressed-background: %2$s color(#dddddd);
                    }
                    button:disabled:host {
                      base-background: %3$s;
                    }
                    .__confirm-button__ {
                      base-background: %4$s;
                      hover-background: %5$s;
                      pressed-background: %5$s color(#dddddd);
                    }
                    .__reject-button__ {
                      base-background: %6$s;
                      hover-background: %7$s;
                      pressed-background: %7$s color(#dddddd);
                    }
                    selector:host {
                      background: %1$s;
                    }
                    .__toggle_button__ {
                      base-background: %8$s;
                      hover-background: %9$s;
                      pressed-background: %10$s;
                    }
                    scroller-horizontal .__scroller_scroll_bar__,
                    scroller-vertical .__scroller_scroll_bar__ {
                      base-background: %1$s;
                      hover-background: %2$s;
                      pressed-background: %2$s color(#dddddd);
                    }
                    .__scroller_view_view-port__ {
                      background: %11$s;
                    }
                    text-field:host,
                    .__tag-field_text-field__,
                    .__search-component_text-field__ {
                      background: %12$s;
                    }
                    """.formatted(btn, pressed, off, green, greenDown, red, redDown, small, smallHover, smallDown,
                    well, field));
        }
        return oreRounded;
    }

    private static final Stylesheet[] GDP_SLOTS = new Stylesheet[2];

    /**
     * GDP's slots. Its sheet styles none, so they fell through to the MC sheet's light vanilla square, which sat on
     * GDP's dark panel like a hole punched through it (the Crop Manager, every machine screen). GDP's own dark inset
     * instead, square or rounded as the corners are.
     */
    public static synchronized Stylesheet gdpSlots(boolean square) {
        int i = square ? 1 : 0;
        if (GDP_SLOTS[i] == null) {
            String sprite = square ? "BORDER1_RT0_DARK" : "BORDER1_RT1_DARK";
            GDP_SLOTS[i] = Stylesheet.parse("""
                    item-slot,
                    .item-slot_bg,
                    fluid-slot,
                    .fluid-slot_bg {
                      background: built-in(ui-gdp:%s);
                    }
                    """.formatted(sprite));
        }
        return GDP_SLOTS[i];
    }

    /** Square corners for GDP: its rounded {@code RECT_RD*} sprites swapped for the square {@code RECT*} ones. */
    public static synchronized Stylesheet gdpSquare() {
        if (gdpSquare == null) {
            gdpSquare = Stylesheet.parse("""
                    button:host,
                    tag-field:host button {
                      base-background: built-in(ui-gdp:RECT);
                      hover-background: built-in(ui-gdp:RECT_LIGHT);
                      pressed-background: built-in(ui-gdp:RECT_DARK);
                    }
                    text-field:host,
                    search-component:host,
                    .__tag-field_text-field__,
                    .__search-component_text-field__,
                    .__text-area_content-view__ {
                      background: built-in(ui-gdp:RECT_SOLID);
                    }
                    text-field:host,
                    search-component:host,
                    text-area:host,
                    selector:host {
                      focus-overlay: sdf(#00000000, 0, 1, #5aa0e6);
                    }
                    selector:host {
                      background: built-in(ui-gdp:RECT_LIGHT);
                    }
                    switch:host {
                      base-background: built-in(ui-gdp:RECT_DARK);
                      pressed-background: sdf(#c8c8c8, 0, 1, #1e1e1e);
                      unmark-background: built-in(ui-gdp:RECT);
                      mark-background: built-in(ui-gdp:RECT);
                    }
                    .__tab-view_tab_content_container__ {
                      background: built-in(ui-gdp:BORDER_THICK);
                    }
                    """);
        }
        return gdpSquare;
    }

    /* ---------------------------------------------- reading ---------------------------------------------- */

    /** Every mod's {@code <path>.lss} for a merged id, LdLib's first, joined — what LdLib merges. */
    private static String read(ResourceLocation mergedId) {
        String file = mergedId.getPath() + ".lss";
        Map<ResourceLocation, List<Resource>> stacks = Minecraft.getInstance().getResourceManager()
                .listResourceStacks(StylesheetManager.PATH, id -> id.getPath().equals(file));
        if (stacks.isEmpty()) return null;
        List<ResourceLocation> order = new ArrayList<>(stacks.keySet());
        order.sort((a, b) -> Boolean.compare(!a.getNamespace().equals(LDLib2.MOD_ID), !b.getNamespace().equals(LDLib2.MOD_ID)));
        StringBuilder out = new StringBuilder();
        for (ResourceLocation id : order) {
            for (Resource resource : stacks.get(id)) {
                try (BufferedReader reader = resource.openAsReader()) {
                    reader.lines().forEach(line -> out.append(line).append('\n'));
                } catch (Exception e) {
                    RollMod.LOGGER.error("[Theme] could not read {} from {}", id, resource.sourcePackId(), e);
                }
            }
        }
        return out.toString();
    }

    /* --------------------------------------------- rewriting --------------------------------------------- */

    private static final Pattern NUMBER = Pattern.compile("\\d+(\\.\\d+)?");

    /** Every {@code sdf(colour, radius…, …)} with each radius number passed through {@code radius}. */
    static String radii(String text, IntUnaryOperator radius) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (true) {
            int start = text.indexOf("sdf(", i);
            if (start < 0) {
                out.append(text, i, text.length());
                return out.toString();
            }
            int open = start + 3;
            int close = matching(text, open);
            if (close < 0) {
                out.append(text, i, text.length());
                return out.toString();
            }
            out.append(text, i, open + 1);
            String[] args = splitArgs(text.substring(open + 1, close));
            if (args.length >= 2) {
                Matcher m = NUMBER.matcher(args[1]);
                StringBuilder r = new StringBuilder();
                while (m.find()) {
                    int value = (int) Math.round(Double.parseDouble(m.group()));
                    m.appendReplacement(r, Integer.toString(radius.applyAsInt(value)));
                }
                m.appendTail(r);
                args[1] = r.toString();
            }
            out.append(String.join(",", args)).append(')');
            i = close + 1;
        }
    }

    private static int matching(String text, int open) {
        int depth = 0;
        for (int j = open; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == '(') depth++;
            else if (c == ')' && --depth == 0) return j;
        }
        return -1;
    }

    /** Splits on commas outside nested parentheses, keeping the spacing. */
    private static String[] splitArgs(String args) {
        List<String> out = new ArrayList<>();
        int depth = 0, from = 0;
        for (int j = 0; j < args.length(); j++) {
            char c = args.charAt(j);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                out.add(args.substring(from, j));
                from = j + 1;
            }
        }
        out.add(args.substring(from));
        return out.toArray(String[]::new);
    }

    /**
     * Dusk's colours (rgb, alpha kept) for Ore UI's, at the brightness of green OreUI (the old Ore art in
     * {@code ore_styles.png}): its #a1a1a1 panels and #585c63 wells, white text over them, in greys instead of green.
     */
    private static final Map<String, String> ORE_RGB = Map.ofEntries(
            Map.entry("0f1116", "585c63"),   // bg: wells, tracks
            Map.entry("171a21", "a1a1a1"),   // surface: panels
            Map.entry("212632", "8d8d8f"),   // raised: controls at rest
            Map.entry("2b3242", "999a9c"),   // hovered
            Map.entry("0b0d11", "4a4b4f"),   // sunken: inputs
            Map.entry("1a1f29", "9a9a9a"),
            Map.entry("1f2430", "9d9d9d"),
            Map.entry("191d26", "5c5d62"),
            Map.entry("e7eaf2", "ffffff"),   // text
            Map.entry("98a0b4", "e0e0e0"),   // dim
            Map.entry("6b7280", "c4c4c4"),   // faint
            Map.entry("7c8cff", "3c8527"),   // accent: Ore UI's green
            Map.entry("99a5ff", "639d52"),
            Map.entry("6774e6", "2a641c"));
    /** The same for OreUI dark: Bedrock's dark mode greys, one step down from the light ones. */
    private static final Map<String, String> ORE_DARK_RGB = Map.ofEntries(
            Map.entry("0f1116", "121212"),
            Map.entry("171a21", "1e1e1f"),
            Map.entry("212632", "2c2c2d"),
            Map.entry("2b3242", "3a3a3b"),
            Map.entry("0b0d11", "0c0c0c"),
            Map.entry("1a1f29", "242425"),
            Map.entry("1f2430", "28282a"),
            Map.entry("191d26", "1a1a1b"),
            Map.entry("e7eaf2", "ffffff"),
            Map.entry("98a0b4", "a0a1a4"),
            Map.entry("6b7280", "77787b"),
            Map.entry("7c8cff", "3c8527"),
            Map.entry("99a5ff", "639d52"),
            Map.entry("6774e6", "2a641c"));
    /** AE2's palette (its {@code screens/common/palette.json} and GUI art): lavender greys, ink text, ME cyan. */
    private static final Map<String, String> AE2_RGB = Map.ofEntries(
            Map.entry("0f1116", "adb0c4"),   // wells, tracks
            Map.entry("171a21", "cbccd4"),   // panels: AE2's background face
            Map.entry("212632", "d6d7de"),   // controls at rest
            Map.entry("2b3242", "e2e3e9"),   // hovered
            Map.entry("0b0d11", "9a9fb4"),   // inputs: AE2's search field
            Map.entry("1a1f29", "b9bccb"),
            Map.entry("1f2430", "c2c4cf"),
            Map.entry("191d26", "a6a9bd"),
            Map.entry("e7eaf2", "413f54"),   // text: AE2's DEFAULT_TEXT_COLOR
            Map.entry("98a0b4", "5d5b73"),
            Map.entry("6b7280", "878fa5"),   // AE2's MUTED_TEXT_COLOR
            Map.entry("7c8cff", "3a8fc4"),   // accent: the ME network's cyan
            Map.entry("99a5ff", "6fc3ef"),
            Map.entry("6774e6", "2e74a0"));
    private static final Map<String, String> AE2_ARGB = Map.of(
            "16ffffff", "ff878fa5",
            "2effffff", "ff413f54");
    /** Dusk's faint white hairlines become Ore UI's dark outlines. */
    private static final Map<String, String> ORE_ARGB = Map.of(
            "16ffffff", "ff585c63",
            "2effffff", "ff1e1e1f");
    /** OreUI dark keeps dark outlines. */
    private static final Map<String, String> ORE_DARK_ARGB = Map.of(
            "16ffffff", "ff2a2a2b",
            "2effffff", "ff1e1e1f");
    private static final Pattern HEX = Pattern.compile("#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})\\b");

    static String orePalette(String text, UiTheme theme) {
        Map<String, String> rgbMap = switch (theme) {
            case OREUI_DARK -> ORE_DARK_RGB;
            case AE2 -> AE2_RGB;
            default -> ORE_RGB;
        };
        Map<String, String> argbMap = theme == UiTheme.AE2 ? AE2_ARGB
                : theme == UiTheme.OREUI_DARK ? ORE_DARK_ARGB : ORE_ARGB;
        Matcher m = HEX.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String hex = m.group(1).toLowerCase(java.util.Locale.ROOT);
            String swapped = argbMap.get(hex);
            if (swapped == null) {
                String alpha = hex.length() == 8 ? hex.substring(0, 2) : "";
                String rgb = rgbMap.get(hex.substring(alpha.length()));
                swapped = rgb == null ? hex : alpha + rgb;
            }
            m.appendReplacement(out, "#" + swapped);
        }
        m.appendTail(out);
        return out.toString();
    }

    /**
     * Ore UI's textured widgets. {@code sprite(texture, x, y, w, h, left, top, right, bottom)} is nine-sliced
     * with those borders (the pack's own .mcmeta ones, at least 2 where the rounded art cuts 2px corners).
     */
    private static String oreUiWidgetsText(boolean square, UiTheme theme) {
        String d = "ldlib2:textures/gui/oreui/" + (square ? "" : "rounded/");
        boolean dark = theme == UiTheme.OREUI_DARK;
        // Dark: every grey widget multiplied by the theme's tint; pressed a step further. One color() each, never stacked.
        String tintHex = tint(theme);
        int base = tintHex == null ? 0xFFFFFF : Integer.parseInt(tintHex, 16);
        String tint = tintHex == null ? "" : " color(#" + tintHex + ")";
        String pressedTint = " color(#" + hex(multiply(base, 0xDDDDDD)) + ")";
        String deepTint = " color(#" + hex(multiply(base, 0xBBBBBB)) + ")";
        // Ore's coloured buttons, kept from the merged Ore theme: the grey art multiplied to Ore UI's green and red.
        String greenTint = " color(#" + hex(multiply(0x7BFF52, dark ? 0xC0C0C0 : 0xFFFFFF)) + ")";
        String red = " color(#" + hex(multiply(0xFF6E6E, dark ? 0xC0C0C0 : 0xFFFFFF)) + ")";
        String field0 = dark ? "#0c0c0c" : "#4a4b4f";
        int r = square ? 0 : 2;
        String button = "sprite(" + d + "button.png, 0, 0, 200, 20, 4, 4, 4, 4)";
        String buttonHover = "sprite(" + d + "button_highlighted.png, 0, 0, 202, 22, 4, 4, 4, 6)";
        String buttonOff = "sprite(" + d + "button_disabled.png, 0, 0, 200, 20, 2, 2, 2, 2)";
        String field = "sprite(" + d + "text_field.png, 0, 0, 200, 20, 2, 3, 2, 2)";
        String handle = "sprite(" + d + "slider_handle.png, 0, 0, 8, 20, 2, 2, 2, 3)";
        String handleHover = "sprite(" + d + "slider_handle_highlighted.png, 0, 0, 8, 20, 2, 2, 2, 3)";
        String box = "sprite(" + d + "checkbox.png, 0, 0, 20, 20, 3, 3, 3, 3)";
        String boxHover = "sprite(" + d + "checkbox_highlighted.png, 0, 0, 20, 20, 3, 3, 3, 3)";
        String boxOn = "sprite(" + d + "checkbox_selected.png, 0, 0, 20, 20, 3, 3, 3, 3)";
        String boxOnHover = "sprite(" + d + "checkbox_selected_highlighted.png, 0, 0, 20, 20, 3, 3, 3, 3)";
        String tab = "sprite(" + d + "tab.png, 0, 0, 130, 24, 2, 2, 2, 0)";
        String tabHover = "sprite(" + d + "tab_highlighted.png, 0, 0, 130, 24, 2, 2, 2, 0)";
        String tabOn = "sprite(" + d + "tab_selected.png, 0, 0, 130, 24, 2, 2, 2, 0)";
        String tabOnHover = "sprite(" + d + "tab_selected_highlighted.png, 0, 0, 130, 24, 2, 2, 2, 0)";
        String bar = "sprite(" + d + "scroller.png, 0, 0, 6, 32, 2, 2, 2, 5)";
        String track = "sprite(" + d + "scroller_background.png, 0, 0, 6, 32, 2, 2, 2, 2)";
        String outline = dark ? "#0c0c0c" : "#2a2a2b";
        String plainButton = button, plainHover = buttonHover;
        field += tint;
        button += tint; buttonHover += tint; buttonOff += tint; handle += tint; handleHover += tint;
        box += tint; boxHover += tint; boxOn += tint; boxOnHover += tint;
        tab += tint; tabHover += tint; tabOn += tint; tabOnHover += tint; bar += tint; track += tint;
        return """
                button:host,
                tag-field:host button,
                color-selector:host button,
                selector:host {
                  base-background: %1$s;
                  hover-background: %2$s;
                  pressed-background: %19$s;
                }
                .__confirm-button__ {
                  base-background: %21$s;
                  hover-background: %22$s;
                  pressed-background: %22$s;
                }
                .__reject-button__ {
                  base-background: %23$s;
                  hover-background: %24$s;
                  pressed-background: %24$s;
                }
                selector:host,
                selector:host:hovered {
                  background: %1$s;
                }
                button:disabled:host {
                  base-background: %3$s;
                }
                button:host .__button_text__,
                .__selector_preview__ label {
                  text-color: #ffffff;
                }
                button:disabled:host .__button_text__ {
                  text-color: #8c8d90;
                }
                text-field:host,
                search-component:host,
                color-selector:host text-field,
                .__tag-field_text-field__,
                .__search-component_text-field__,
                .__text-area_content-view__ {
                  background: %4$s;
                }
                text-field:host,
                search-component:host,
                text-area:host,
                color-selector:host text-field {
                  focus-overlay: sdf(#00000000, %17$d, 1, %28$s);
                }
                .__slider_track__ {
                  background: sdf(%27$s, %17$d, 1, %18$s);
                }
                .__slider_fill__ {
                  background: sdf(#3c8527, %17$d);
                }
                .__slider_handle__ {
                  base-background: %5$s;
                  hover-background: %6$s;
                  pressed-background: %20$s;
                }
                .__toggle_button__ {
                  base-background: %7$s;
                  hover-background: %8$s;
                  pressed-background: %8$s;
                }
                toggle:on .__toggle_button__ {
                  base-background: %9$s;
                  hover-background: %10$s;
                  pressed-background: %10$s;
                }
                toggle:host {
                  mark-background: empty;
                }
                tab:host {
                  base-background: %11$s;
                  hover-background: %12$s;
                  pressed-background: %13$s;
                }
                tab:host:selected {
                  base-background: %13$s;
                  hover-background: %14$s;
                  pressed-background: %13$s;
                }
                .__scroller_scroll_container__ {
                  background: %16$s;
                }
                scroller-horizontal .__scroller_scroll_bar__,
                scroller-vertical .__scroller_scroll_bar__ {
                  base-background: %15$s;
                  hover-background: %25$s;
                  pressed-background: %26$s;
                }
                .__progress-bar_bar-container__ {
                  background: sdf(%27$s, %17$d, 1, %18$s);
                }
                .__progress-bar_bar__ {
                  background: sdf(#3c8527, %17$d);
                }
                """.formatted(button, buttonHover, buttonOff, field, handle, handleHover, box, boxHover, boxOn, boxOnHover,
                tab, tabHover, tabOn, tabOnHover, bar, track, r, outline,
                plainHover + pressedTint,                                   // 19 button pressed
                handleHover.replace(tint, "") + pressedTint,                 // 20 slider handle pressed
                plainButton + greenTint, plainHover + greenTint,             // 21, 22 confirm
                plainButton + red, plainHover + red,                         // 23, 24 reject
                bar.replace(tint, "") + pressedTint, bar.replace(tint, "") + deepTint, // 25, 26 scroll bar
                field0,                                                      // 27 wells
                "#ffffff");                                                  // 28 focus outline
    }

    /** Two RGB colours multiplied, as a texture tint does. */
    private static int multiply(int a, int b) {
        int r = ((a >> 16) & 0xFF) * ((b >> 16) & 0xFF) / 255;
        int g = ((a >> 8) & 0xFF) * ((b >> 8) & 0xFF) / 255;
        int bl = (a & 0xFF) * (b & 0xFF) / 255;
        return (r << 16) | (g << 8) | bl;
    }

    private static String hex(int rgb) {
        return String.format("%06x", rgb & 0xFFFFFF);
    }
}
