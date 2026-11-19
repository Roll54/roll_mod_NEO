package com.roll_54.roll_mod.minestar.ui;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * A remembered window rectangle and maximize state in its own file under {@code config/roll_mod/}
 * — the plain {@link WindowGeometry} for windows with no other settings to keep. One instance per file,
 * loaded on first use.
 */
public final class SavedWindowState implements WindowGeometry {

    private static final Map<String, SavedWindowState> BY_FILE = new HashMap<>();

    private final String fileName;
    public final int defaultW;
    public final int defaultH;
    public final int minW;
    public final int minH;

    /** GUI-scaled pixels. {@code x < 0} means "never placed": centre it. */
    private int x = -1;
    private int y = -1;
    private int w;
    private int h;
    private boolean maximized;
    private boolean dirty;

    private SavedWindowState(String fileName, int defaultW, int defaultH, int minW, int minH) {
        this.fileName = fileName;
        this.defaultW = defaultW;
        this.defaultH = defaultH;
        this.minW = minW;
        this.minH = minH;
        this.w = defaultW;
        this.h = defaultH;
    }

    /** The state kept in {@code config/roll_mod/<fileName>}; the sizes apply the first time it is asked for. */
    public static synchronized SavedWindowState get(String fileName, int defaultW, int defaultH, int minW, int minH) {
        return BY_FILE.computeIfAbsent(fileName, f -> load(new SavedWindowState(f, defaultW, defaultH, minW, minH)));
    }

    @Override
    public Rect applied(int screenW, int screenH) {
        return WindowGeometry.fit(x, y, w, h, maximized, minW, minH, screenW, screenH);
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
        setRect(-1, -1, defaultW, defaultH);
        setMaximized(false);
    }

    @Override
    public synchronized void save() {
        if (!dirty) return;
        JsonObject json = new JsonObject();
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("width", w);
        json.addProperty("height", h);
        json.addProperty("maximized", maximized);
        Path path = file(fileName);
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            MinestarFiles.write(tmp, json);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            dirty = false;
        } catch (IOException e) {
            RollMod.LOGGER.error("[UI] could not save {}", path, e);
        }
    }

    private static SavedWindowState load(SavedWindowState state) {
        JsonObject json = MinestarFiles.readObject(file(state.fileName));
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

    private static Path file(String name) {
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve(name);
    }
}
