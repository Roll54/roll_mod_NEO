package com.roll_54.roll_mod.minestar.fzzyui;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import com.roll_54.roll_mod.minestar.ui.WindowGeometry;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Where the player last left the config window, and how big — one rectangle for every config screen.
 * Client state, kept in {@code config/roll_mod/config_window.json}. Same shape as
 * {@code HubWindowState}, without the hub's own settings.
 */
final class ConfigWindowState implements WindowGeometry {

    static final int DEFAULT_W = 440;
    static final int DEFAULT_H = 290;
    /** Room for the page list, a name column and an editor. */
    static final int MIN_W = 320;
    static final int MIN_H = 180;

    private static final int VERSION = 1;

    /** GUI-scaled pixels. {@code x < 0} means "never placed": centre it. */
    private int x = -1;
    private int y = -1;
    private int w = DEFAULT_W;
    private int h = DEFAULT_H;
    private boolean maximized;
    private boolean dirty;

    private static ConfigWindowState instance;

    private ConfigWindowState() {}

    static synchronized ConfigWindowState get() {
        if (instance == null) instance = load();
        return instance;
    }

    @Override
    public Rect applied(int screenW, int screenH) {
        return WindowGeometry.fit(x, y, w, h, maximized, MIN_W, MIN_H, screenW, screenH);
    }

    @Override
    public boolean maximized() {
        return maximized;
    }

    @Override
    public void setMaximized(boolean maximized) {
        if (this.maximized == maximized) return;
        this.maximized = maximized;
        dirty = true;
    }

    @Override
    public void setRect(int x, int y, int w, int h) {
        if (this.x == x && this.y == y && this.w == w && this.h == h) return;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        dirty = true;
    }

    @Override
    public void reset() {
        setRect(-1, -1, DEFAULT_W, DEFAULT_H);
        setMaximized(false);
    }

    @Override
    public synchronized void save() {
        if (!dirty) return;
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("width", w);
        json.addProperty("height", h);
        json.addProperty("maximized", maximized);
        Path path = file();
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            MinestarFiles.write(tmp, json);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            dirty = false;
        } catch (IOException e) {
            RollMod.LOGGER.error("[Config UI] could not save {}", path, e);
        }
    }

    private static ConfigWindowState load() {
        ConfigWindowState state = new ConfigWindowState();
        JsonObject json = MinestarFiles.readObject(file());
        state.x = intOr(json, "x", state.x);
        state.y = intOr(json, "y", state.y);
        state.w = Math.max(1, intOr(json, "width", state.w));
        state.h = Math.max(1, intOr(json, "height", state.h));
        state.maximized = json.has("maximized") && json.get("maximized").isJsonPrimitive()
                && json.get("maximized").getAsBoolean();
        return state;
    }

    private static int intOr(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("config_window.json");
    }
}
