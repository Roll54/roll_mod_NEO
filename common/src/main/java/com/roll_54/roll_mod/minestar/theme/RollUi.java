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
        return out;
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
