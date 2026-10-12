package com.roll_54.roll_mod.minestar.hub.gui;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

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

    private static final int VERSION = 3;

    /** GUI-scaled pixels. {@code x < 0} means "never placed": centre it. */
    private int x = -1;
    private int y = -1;
    private int w = DEFAULT_W;
    private int h = DEFAULT_H;
    private boolean maximized;
    /** Which edge the tab bookmarks sit along. */
    private HubTabSide tabSide = HubTabSide.TOP;
    /** Whether the window can be dragged around by its title band. */
    private boolean movable = true;
    /** Whether the desktop windows the hub opened (info, popups) go when the hub screen does. */
    private boolean closeWindowsWithHub = true;
    /** Each tab's list width where the player dragged its divider — see {@link HubSplit}. */
    private final java.util.Map<String, Integer> splits = new java.util.HashMap<>();
    /**
     * What each account on this machine pinned to the top of its lists: account → kind → ids, in
     * the order they were pinned. Per account, because two players sharing a PC pin different
     * things; client-only by choice, so pins do not follow a player to another machine.
     */
    private final java.util.Map<UUID, java.util.Map<PinKind, java.util.Set<UUID>>> pins = new java.util.HashMap<>();
    /** Bumped on every pin change, for the tabs' rebuild signatures. */
    private int pinVersion;

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

    public HubTabSide tabSide() {
        return tabSide;
    }

    public void setTabSide(HubTabSide side) {
        if (tabSide == side) return;
        tabSide = side;
        dirty = true;
    }

    public boolean closeWindowsWithHub() {
        return closeWindowsWithHub;
    }

    public void setCloseWindowsWithHub(boolean close) {
        if (this.closeWindowsWithHub == close) return;
        this.closeWindowsWithHub = close;
        dirty = true;
    }

    public boolean movable() {
        return movable;
    }

    public void setMovable(boolean movable) {
        if (this.movable == movable) return;
        this.movable = movable;
        dirty = true;
    }

    /** The list width saved for a tab's divider, or {@code fallback} when it was never dragged. */
    public int split(String key, int fallback) {
        return splits.getOrDefault(key, fallback);
    }

    /** Remembers a divider's list width; {@code -1} forgets it, back to the tab's default. */
    public void setSplit(String key, int width) {
        Integer old = width < 0 ? splits.remove(key) : splits.put(key, width);
        if (old == null || old != width) dirty = true;
    }

    /** The lists a player can pin entries to the top of. */
    public enum PinKind { HOMES, WARPS, PLAYERS }

    public boolean isPinned(UUID viewer, PinKind kind, UUID id) {
        var byKind = pins.get(viewer);
        return byKind != null && byKind.getOrDefault(kind, java.util.Set.of()).contains(id);
    }

    /** Pins or unpins {@code id} for {@code viewer} and writes the file at once — a pin is a decision, not a drag. */
    public synchronized void setPinned(UUID viewer, PinKind kind, UUID id, boolean pinned) {
        java.util.Set<UUID> ids = pins.computeIfAbsent(viewer, v -> new java.util.EnumMap<>(PinKind.class))
                .computeIfAbsent(kind, k -> new java.util.LinkedHashSet<>());
        if (pinned ? !ids.add(id) : !ids.remove(id)) return;
        pinVersion++;
        dirty = true;
        save();
    }

    public int pinVersion() {
        return pinVersion;
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
        json.addProperty("tabSide", tabSide.name());
        json.addProperty("movable", movable);
        json.addProperty("closeWindowsWithHub", closeWindowsWithHub);
        JsonObject splitJson = new JsonObject();
        splits.forEach(splitJson::addProperty);
        json.add("splits", splitJson);
        JsonObject pinJson = new JsonObject();
        pins.forEach((viewer, byKind) -> {
            JsonObject kinds = new JsonObject();
            byKind.forEach((kind, ids) -> {
                if (ids.isEmpty()) return;
                com.google.gson.JsonArray array = new com.google.gson.JsonArray();
                ids.forEach(id -> array.add(id.toString()));
                kinds.add(kind.name().toLowerCase(java.util.Locale.ROOT), array);
            });
            if (!kinds.isEmpty()) pinJson.add(viewer.toString(), kinds);
        });
        json.add("pins", pinJson);

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
        // Version 2 additions; a version-1 file simply has none of them and keeps the defaults.
        if (json.has("tabSide") && json.get("tabSide").isJsonPrimitive()) {
            state.tabSide = HubTabSide.byName(json.get("tabSide").getAsString());
        }
        if (json.has("closeWindowsWithHub") && json.get("closeWindowsWithHub").isJsonPrimitive()) {
            state.closeWindowsWithHub = json.get("closeWindowsWithHub").getAsBoolean();
        }
        if (json.has("movable") && json.get("movable").isJsonPrimitive()) {
            state.movable = json.get("movable").getAsBoolean();
        }
        if (json.has("splits") && json.get("splits").isJsonObject()) {
            for (var entry : json.getAsJsonObject("splits").entrySet()) {
                try {
                    state.splits.put(entry.getKey(), entry.getValue().getAsInt());
                } catch (RuntimeException ignored) {
                    // A hand-edited value that is not a number: that divider keeps its default.
                }
            }
        }
        // Version 3: pins. Anything unreadable is dropped rather than failing the whole file.
        if (json.has("pins") && json.get("pins").isJsonObject()) {
            for (var viewer : json.getAsJsonObject("pins").entrySet()) {
                try {
                    UUID viewerId = UUID.fromString(viewer.getKey());
                    for (var kind : viewer.getValue().getAsJsonObject().entrySet()) {
                        PinKind pinKind = PinKind.valueOf(kind.getKey().toUpperCase(java.util.Locale.ROOT));
                        java.util.Set<UUID> ids = state.pins
                                .computeIfAbsent(viewerId, v -> new java.util.EnumMap<>(PinKind.class))
                                .computeIfAbsent(pinKind, k -> new java.util.LinkedHashSet<>());
                        for (var id : kind.getValue().getAsJsonArray()) ids.add(UUID.fromString(id.getAsString()));
                    }
                } catch (RuntimeException ignored) {
                    // A hand-edited entry that does not parse: that account's pins start over.
                }
            }
        }
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
