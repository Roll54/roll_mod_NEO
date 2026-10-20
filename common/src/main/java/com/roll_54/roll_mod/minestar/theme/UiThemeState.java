package com.roll_54.roll_mod.minestar.theme;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The player's chosen {@link UiTheme}: client state, kept in {@code config/roll_mod/ui_theme.json}
 * so it survives a restart. Same shape as {@code HubWindowState}.
 *
 * <p>Only ever read for a client-side copy of a screen — see {@link RollUi#sheets}. The dedicated
 * server builds every screen too, and must never touch one player's file.
 */
public final class UiThemeState {

    private static final int VERSION = 1;

    private static UiTheme current;
    /** Experimental: the theme also on every other mod's LdLib screen — see {@code ForceThemeHandler}. */
    private static boolean forceEverywhere;

    private UiThemeState() {}

    /** The theme, read from disk on first use and kept in memory for the rest of the session. */
    public static synchronized UiTheme get() {
        load();
        return current;
    }

    public static synchronized boolean forceEverywhere() {
        load();
        return forceEverywhere;
    }

    public static synchronized void setForceEverywhere(boolean force) {
        load();
        if (forceEverywhere == force) return;
        forceEverywhere = force;
        save();
    }

    private static void load() {
        if (current != null) return;
        JsonObject json = MinestarFiles.readObject(file());
        current = json.has("theme") && json.get("theme").isJsonPrimitive()
                ? UiTheme.byId(json.get("theme").getAsString())
                : UiTheme.DEFAULT;
        forceEverywhere = json.has("forceEverywhere") && json.get("forceEverywhere").isJsonPrimitive()
                && json.get("forceEverywhere").getAsBoolean();
    }

    public static synchronized void set(UiTheme theme) {
        if (get() == theme) return;
        current = theme;
        save();
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        json.addProperty("theme", current.id());
        json.addProperty("forceEverywhere", forceEverywhere);

        Path path = file();
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            MinestarFiles.write(tmp, json);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Theme] could not save {}", path, e);
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("ui_theme.json");
    }
}
