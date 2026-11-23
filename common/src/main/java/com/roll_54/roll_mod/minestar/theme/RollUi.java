package com.roll_54.roll_mod.minestar.theme;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleEngine;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Where every LdLib screen of the mod gets its stylesheets, so they all wear the theme the player
 * picked in the hub's settings ({@link UiThemeState}).
 *
 * <p>The theme is applied to the client copy of a screen only. The server builds the same tree but
 * never draws it, and on a single-player server reading the preference would read the client's file
 * from the server thread for nothing.
 */
public final class RollUi {

    private RollUi() {}

    public static UI of(Player player, UIElement root) {
        return UI.of(root, sheets(player));
    }

    public static UI of(Player player, UIElement root, UI.DynamicSizeProvider size) {
        return UI.of(root, sheets(player), size);
    }

    /** The current theme's sheets for {@code player}'s copy of a screen; none on the server's copy. */
    public static List<Stylesheet> sheets(Player player) {
        return player.level().isClientSide() ? sheets() : List.of();
    }

    /** The current theme's sheets. Client only. */
    public static List<Stylesheet> sheets() {
        List<Stylesheet> out = new ArrayList<>();
        UiTheme theme = UiThemeState.get();
        boolean square = theme.hasCorners() && UiThemeState.squareCorners();
        for (ResourceLocation id : theme.sheets()) {
            out.add(ThemeSheets.sheet(id, theme, square));
        }
        if (theme.oreUi()) out.add(ThemeSheets.oreUiWidgets(square, theme));
        if (theme == UiTheme.AE2) out.add(ThemeSheets.ae2Widgets(square));
        if (theme.oreArt() && !square) out.add(ThemeSheets.oreRounded());
        if (theme == UiTheme.GDP && square) out.add(ThemeSheets.gdpSquare());
        // GDP's slots as the player's own inventory draws them (vanilla's bevel in GDP's recolour).
        if (theme == UiTheme.GDP) {
            Stylesheet slots = ThemeSheets.vanillaSlots(theme);
            if (slots != null) out.add(slots);
        }
        // The font from Settings, over the theme's own (UiFont); none for "as the theme".
        com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet font = UiFont.sheet();
        if (font != null) out.add(font);
        // Last: the same button size on every theme, and the switch (see geometry).
        out.add(geometry(theme, square));
        return out;
    }

    /**
     * What no theme may change: a button's size (the themes give buttons their own height and padding, so a
     * button sized by its content grew or shrank with the theme, unlike everything else), and the switch, which is
     * GDP's on every theme but Ore UI. Only values an element did not set itself: a size set in code is inline and
     * still wins.
     *
     * <p>Ore UI keeps its own buttons and switch, drawn as Bedrock's Ore UI draws them: a raised face over a dark
     * bottom edge, the caption on the face (lifted off the edge, since the hub sets most buttons' padding in code),
     * dark grey text — vanilla's label grey #404040, the same as assets/roll_mod/lss/ore.lss gives the rest of Ore's
     * text, rather than near-black; the I/O switch. Its buttons are as tall as everywhere else.
     */
    private static Stylesheet oreGeometry;
    /**
     * Sizes every theme shares, so a screen is laid out the same whichever is picked: the themes gave scroll bars
     * 5 to 14px, scroll areas 8 to 10px of margin and their inner padding 2 to 3px, so content moved and lists lost
     * or gained width when switching. Only what an element does not set itself; a size set in code still wins.
     * Tabs are left to each theme: the hub's bookmarks are tabs too, and a shared 16px height cropped their icons.
     * No single-side paddings here: a sheet's padding-top/bottom outranks a padding-all set in code, so they undid
     * the hub's paddingAll(0) (the bookmark strip lost 6px and cut its icons).
     */
    private static final String SHARED = """
            scroller-horizontal:host {
              height: 6;
            }
            scroller-vertical:host {
              width: 6;
            }
            scroller-view,
            text-area {
              scroller-view-margin: 8;
            }
            .__scroller_view_view-port__ {
              padding-all: 3;
            }
            .__scroller_view_horizontal-scroller__,
            .__text-area_horizontal-scroller__ {
              height: 6;
              margin-top: 2;
            }
            .__scroller_view_vertical-scroller__,
            .__text-area_vertical-scroller__ {
              width: 6;
              margin-left: 2;
            }
            .__scroller_scroll_container__ {
              padding-all: 0;
            }
            .__scroller_head_button__,
            .__scroller_tail_button__ {
              display: none;
            }
            .panel_bg {
              padding-all: 5;
              gap-all: 4;
            }
            text-field:host,
            .__tag-field_text-field__,
            .__search-component_text-field__,
            .__text-area_content-view__,
            color-selector:host text-field {
              padding-all: 3;
            }
            search-component:host,
            selector:host,
            color-selector:host text-field {
              height: 16;
            }
            tag-field:host button,
            color-selector:host button {
              padding-all: 2;
            }
            progress-bar:host {
              height: 8;
            }
            slider-horizontal:host {
              height: 10;
            }
            slider-vertical:host {
              width: 10;
            }
            toggle:host {
              height: 14;
            }
            .__toggle_button__ {
              padding-all: 0;
            }
            button:host {
              height: 14;
              padding-all: 2;
            }
            switch:host {
              width: 26;
              height: 14;
            }
            """;
    /** By [square][0 for any non-OreUI theme, else 1 + the OreUI theme's ordinal]. */
    private static final Stylesheet[][] GEOMETRY = new Stylesheet[2][1 + UiTheme.values().length];
    /** {@link ThemeSheets#generation()} the cached geometry was parsed at: older ones are rebuilt, so they stay newer than the theme. */
    private static int geometryGeneration = -1;

    private static Stylesheet geometry(UiTheme theme, boolean square) {
        if (geometryGeneration != ThemeSheets.generation()) {
            geometryGeneration = ThemeSheets.generation();
            oreGeometry = null;
            for (Stylesheet[] row : GEOMETRY) java.util.Arrays.fill(row, null);
        }
        if (theme.oreArt()) {
            if (oreGeometry == null) {
                oreGeometry = Stylesheet.parse(SHARED + """
                        button:host {
                          height: 14;
                          padding-all: 2;
                          padding-top: 1;
                          padding-bottom: 3;
                        }
                        button:host .__button_text__ {
                          text-color: #404040;
                          text-shadow: false;
                        }
                        button:disabled:host .__button_text__ {
                          text-color: #6b6b6b;
                        }
                        switch:host {
                          padding-all: 0;
                        }
                        text-field,
                        text-area {
                          text-color: #ffffff;
                          cursor-color: #ffffff;
                        }
                        """);
            }
            return oreGeometry;
        }
        boolean oreui = theme.recoloured();
        int variant = oreui ? 1 + theme.ordinal() : 0;
        Stylesheet cached = GEOMETRY[square ? 1 : 0][variant];
        if (cached != null) return cached;
        String switchStyle;
        if (theme == UiTheme.AE2) {
            // GDP's switch in AE2's colours; the caption lifted off the button's 3px bottom bevel.
            switchStyle = """
                    switch:host {
                      base-background: sdf(#9a9fb4, %1$d, 1, #413f54);
                      pressed-background: sdf(#3a8fc4, %1$d, 1, #413f54);
                      unmark-background: sdf(#f2f2f2, %1$d, 1, #413f54);
                      mark-background: sdf(#f2f2f2, %1$d, 1, #413f54);
                      width: 26;
                      height: 14;
                      padding-all: 2;
                    }
                    button:host .__button_text__ {
                      padding-bottom: 2;
                    }
                    .__button_pre-icon__,
                    .__button_post-icon__ {
                      width: 10;
                      height: 10;
                      margin-bottom: 2;
                    }
                    """.formatted(square ? 0 : 2);
        } else if (oreui) {
            // Ore's own I/O switch (LdLib's ui-ore sprites), as the merged Ore theme had it. Dark dims it like the
            // rest of its widgets.
            String dim = theme == UiTheme.OREUI_DARK ? " color(#" + ThemeSheets.DARK_TINT + ")" : "";
            switchStyle = """
                    switch:host {
                      base-background: built-in(ui-ore:SWITCH_OFF)%1$s;
                      pressed-background: built-in(ui-ore:SWITCH_ON)%1$s;
                      unmark-background: built-in(ui-ore:BTN_DEFAULT)%1$s;
                      mark-background: built-in(ui-ore:BTN_PRESSED)%1$s;
                      padding-all: 0;
                      width: 26;
                      height: 14;
                    }
                    button:host .__button_text__ {
                      padding-bottom: 2;
                    }
                    """.formatted(dim);
        } else {
            String rd = square ? "RECT" : "RECT_RD";
            switchStyle = """
                    switch:host {
                      base-background: built-in(ui-gdp:%1$s_DARK);
                      pressed-background: %2$s;
                      unmark-background: built-in(ui-gdp:%1$s);
                      mark-background: built-in(ui-gdp:%1$s);
                      width: 26;
                      height: 14;
                      padding-all: 2;
                    }
                    """.formatted(rd, square
                            // GDP has no square see-through light box; RECT_LIGHT is a dark grey, so "on" looked off.
                            ? "sdf(#c8c8c8, 0, 1, #1e1e1e)" : "built-in(ui-gdp:RECT_RD_T)");
        }
        Stylesheet sheet = Stylesheet.parse(SHARED + """
                button:host {
                  height: 14;
                  padding-all: 2;
                }
                """ + switchStyle);
        GEOMETRY[square ? 1 : 0][variant] = sheet;
        return sheet;
    }

    /**
     * Whether text drawn in the theme's own text colour should get a drop shadow: yes on the dark themes, no on
     * the light ones (Paper, Latte), where a shadow under dark text only smudges it. Read when a label is built,
     * so a theme picked while a screen is open applies to it the next time it opens. Always on the server's copy.
     */
    public static boolean textShadow() {
        if (net.neoforged.fml.loading.FMLEnvironment.dist != net.neoforged.api.distmarker.Dist.CLIENT) return true;
        UiTheme theme = UiThemeState.get();
        return theme != UiTheme.PAPER && theme != UiTheme.LATTE && theme != UiTheme.AE2;
    }

    /** Remembers {@code theme} and restyles {@code live}, the screen it was picked on, right away. */
    public static void set(UiTheme theme, @Nullable ModularUI live) {
        UiThemeState.set(theme);
        apply(live);
    }

    /** Swaps an open screen over to the current theme. */
    public static void apply(@Nullable ModularUI ui) {
        if (ui == null) return;
        StyleEngine engine = ui.getStyleEngine();
        engine.clearAllStylesheets();
        engine.addStylesheets(sheets());
        engine.scheduleFullReload();
    }
}
