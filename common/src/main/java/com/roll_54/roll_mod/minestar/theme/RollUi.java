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
        for (ResourceLocation id : UiThemeState.get().sheets()) {
            out.add(StylesheetManager.INSTANCE.getStylesheetSafe(id));
        }
        // The font from Settings, over the theme's own (UiFont); none for "as the theme".
        com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet font = UiFont.sheet();
        if (font != null) out.add(font);
        // Last: the same button size on every theme, and the switch (see geometry).
        out.add(geometry(UiThemeState.get()));
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
    private static Stylesheet geometry;
    private static Stylesheet oreGeometry;

    private static Stylesheet geometry(UiTheme theme) {
        if (theme == UiTheme.ORE) {
            if (oreGeometry == null) {
                oreGeometry = Stylesheet.parse("""
                        button:host {
                          height: 14;
                          padding-all: 2;
                        }
                        button:host .__button_text__ {
                          top: -1;
                          text-color: #404040;
                          text-shadow: false;
                        }
                        button:disabled:host .__button_text__ {
                          text-color: #6b6b6b;
                        }
                        switch:host {
                          padding-all: 0;
                        }
                        """);
            }
            return oreGeometry;
        }
        if (geometry == null) {
            geometry = Stylesheet.parse("""
                    button:host {
                      height: 14;
                      padding-all: 2;
                    }
                    switch:host {
                      base-background: built-in(ui-gdp:RECT_RD_DARK);
                      pressed-background: built-in(ui-gdp:RECT_RD_T);
                      unmark-background: built-in(ui-gdp:RECT_RD);
                      mark-background: built-in(ui-gdp:RECT_RD);
                      width: 26;
                      height: 14;
                      padding-all: 2;
                    }
                    """);
        }
        return geometry;
    }

    /**
     * Whether text drawn in the theme's own text colour should get a drop shadow: yes on the dark themes, no on
     * the light ones (Paper, Latte), where a shadow under dark text only smudges it. Read when a label is built,
     * so a theme picked while a screen is open applies to it the next time it opens. Always on the server's copy.
     */
    public static boolean textShadow() {
        if (net.neoforged.fml.loading.FMLEnvironment.dist != net.neoforged.api.distmarker.Dist.CLIENT) return true;
        UiTheme theme = UiThemeState.get();
        return theme != UiTheme.PAPER && theme != UiTheme.LATTE;
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
