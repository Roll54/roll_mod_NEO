package com.roll_54.roll_mod.minestar.hub.gui;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Where the player last left the hub window, and how big: client state, kept in
 * {@code config/roll_mod/hub_window.json} so it survives a restart.
 *
 * <p>The stored rectangle is a <em>preference</em>, never rewritten to fit the current screen.
 * {@link #applied} clamps a copy, so dropping to a smaller resolution or a bigger GUI scale and
 * going back restores the window exactly as it was.
 *
 * <p>Only the client copy of the hub reads or writes this — see {@link HubWindow}. The dedicated
 * server builds the same tree for every player and must never touch one player's file.
 */
public final class HubWindowState {

    /** The window as it has always been: the 460x250 tab box plus the frame around it. */
    public static final int DEFAULT_W = 472;
    public static final int DEFAULT_H = 296;

    /**
     * Small enough to matter, large enough that every tab still works: the daily-task row plus its
     * bonus panel needs ~390px of width, and the moderation popup ~214px of content height.
     */
    public static final int MIN_W = 420;
    public static final int MIN_H = 260;

    private static final int VERSION = 1;

    /** GUI-scaled pixels. {@code x < 0} means "never placed": centre it. */
    private int x = -1;
    private int y = -1;
    private int w = DEFAULT_W;
    private int h = DEFAULT_H;
    private boolean maximized;

    private boolean dirty;

    private static HubWindowState instance;

    private HubWindowState() {}

    /** The state, read from disk on first use and kept in memory for the rest of the session. */
    public static synchronized HubWindowState get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /** A rectangle in GUI-scaled screen pixels. */
    public record Rect(int x, int y, int w, int h) {}

    /**
     * The window to show on a {@code screenW}x{@code screenH} screen: the preference, shrunk to fit
     * and pushed back on screen. Pure — the preference itself is left alone.
     */
    public Rect applied(int screenW, int screenH) {
        if (maximized) return new Rect(0, 0, screenW, screenH);
        int cw = clamp(w, Math.min(MIN_W, screenW), screenW);
        int ch = clamp(h, Math.min(MIN_H, screenH), screenH);
        int cx = x < 0 ? (screenW - cw) / 2 : clamp(x, 0, screenW - cw);
        int cy = x < 0 ? (screenH - ch) / 2 : clamp(y, 0, screenH - ch);
        return new Rect(cx, cy, cw, ch);
    }

    public boolean maximized() {
        return maximized;
    }

    public void setMaximized(boolean maximized) {
        if (this.maximized == maximized) return;
        this.maximized = maximized;
        dirty = true;
    }

    /** Records where the window now is. Only meaningful while not maximized. */
    public void setRect(int x, int y, int w, int h) {
        if (this.x == x && this.y == y && this.w == w && this.h == h) return;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        dirty = true;
    }

    /** Default size, centred, not maximized. */
    public void reset() {
        setRect(-1, -1, DEFAULT_W, DEFAULT_H);
        setMaximized(false);
    }

    /** Writes the file if anything changed since it was read or last written. */
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
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            dirty = false;
        } catch (IOException e) {
            RollMod.LOGGER.error("[Hub] could not save {}", path, e);
        }
    }

    /**
     * A missing or broken file reads as defaults. A broken one is logged by {@code readObject} and
     * left alone until the window next moves.
     */
    private static HubWindowState load() {
        HubWindowState state = new HubWindowState();
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
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("hub_window.json");
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : Math.min(v, max);
    }
}
