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
    /**
     * Square corners instead of rounded ones — see {@link ThemeSheets}. Kept per theme: the Ore themes are square
     * unless the player rounds them, every other theme rounded unless squared ({@link #squareByDefault}).
     */
    private static final java.util.Map<UiTheme, Boolean> squareCorners = new java.util.EnumMap<>(UiTheme.class);
    /** The AE2 theme's moving lights ({@link MeLights}); on unless switched off. */
    private static boolean meLights = true;

    /** Bumped whenever a setting here changes, so per-frame readers can cache what they derive from them. */
    private static volatile int version;

    private UiThemeState() {}

    /** Changes with every theme, corners or "everywhere" change; compare it to know a cached reading is stale. */
    public static int version() {
        return version;
    }

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
        version++;
        save();
    }

    /** Whether the current theme draws square corners. */
    public static synchronized boolean squareCorners() {
        load();
        return squareCorners.getOrDefault(current, squareByDefault(current));
    }

    /** Sets square corners for the current theme only. */
    public static synchronized void setSquareCorners(boolean square) {
        load();
        if (squareCorners() == square) return;
        squareCorners.put(current, square);
        version++;
        save();
    }

    /** The Ore themes and AE2 are pixel art, square by nature; the rest start rounded. */
    private static boolean squareByDefault(UiTheme theme) {
        return theme.oreArt() || theme == UiTheme.OREUI_DARK || theme == UiTheme.AE2;
    }

    public static synchronized boolean meLights() {
        load();
        return meLights;
    }

    public static synchronized void setMeLights(boolean on) {
        load();
        if (meLights == on) return;
        meLights = on;
        version++;
        save();
    }

    private static void load() {
        if (current != null) return;
        JsonObject json = MinestarFiles.readObject(file());
        current = json.has("theme") && json.get("theme").isJsonPrimitive()
                ? UiTheme.byId(json.get("theme").getAsString())
                : UiTheme.DEFAULT;
        // On unless the player switched it off: a missing or malformed entry means on.
        forceEverywhere = !json.has("forceEverywhere") || !json.get("forceEverywhere").isJsonPrimitive()
                || json.get("forceEverywhere").getAsBoolean();
        squareCorners.clear();
        if (json.has("squareCornersByTheme") && json.get("squareCornersByTheme").isJsonObject()) {
            for (var entry : json.getAsJsonObject("squareCornersByTheme").entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) continue;
                squareCorners.put(UiTheme.byId(entry.getKey()), entry.getValue().getAsBoolean());
            }
        } else if (json.has("squareCorners") && json.get("squareCorners").isJsonPrimitive()) {
            // Before corners were kept per theme: the one choice carries over to the theme in use.
            squareCorners.put(current, json.get("squareCorners").getAsBoolean());
        }
        meLights = !json.has("meLights") || !json.get("meLights").isJsonPrimitive() || json.get("meLights").getAsBoolean();
        version++;
    }

    public static synchronized void set(UiTheme theme) {
        if (get() == theme) return;
        current = theme;
        // Picking a theme puts its corners back to that theme's own: square for the Ore themes and AE2, rounded
        // for the rest. Toggling the switch afterwards still holds until the theme is picked again.
        squareCorners.remove(theme);
        version++;
        save();
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        json.addProperty("theme", current.id());
        json.addProperty("forceEverywhere", forceEverywhere);
        JsonObject corners = new JsonObject();
        squareCorners.forEach((theme, square) -> corners.addProperty(theme.id(), square));
        json.add("squareCornersByTheme", corners);
        json.addProperty("meLights", meLights);

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
