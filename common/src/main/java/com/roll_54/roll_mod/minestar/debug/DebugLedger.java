package com.roll_54.roll_mod.minestar.debug;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What {@code /rollmod debug fill} created for one player, and what it overwrote, so
 * {@code /rollmod debug clear} can undo exactly that and nothing else — even after a restart.
 *
 * <p>Kept in {@code minestar/debug_fill.json}, keyed by the target's UUID. Created entries append on
 * every fill; overwritten values ({@code prior.*}) are recorded only by the first fill, so a second
 * fill cannot replace the player's real balance with the test one.
 */
final class DebugLedger {

    private static final String FILE = "debug_fill.json";

    private final JsonObject root;
    private final String key;
    private final JsonObject entry;

    private DebugLedger(JsonObject root, UUID target) {
        this.root = root;
        this.key = target.toString();
        JsonElement existing = root.get(key);
        this.entry = existing != null && existing.isJsonObject() ? existing.getAsJsonObject() : new JsonObject();
    }

    static DebugLedger load(UUID target) {
        return new DebugLedger(MinestarFiles.readObject(path()), target);
    }

    boolean isEmpty() {
        return entry.size() == 0;
    }

    /* ------------------------------------------- created -------------------------------------------- */

    void add(String list, String value) {
        JsonArray array = entry.has(list) && entry.get(list).isJsonArray()
                ? entry.getAsJsonArray(list) : new JsonArray();
        array.add(value);
        entry.add(list, array);
    }

    void add(String list, UUID value) {
        add(list, value.toString());
    }

    List<String> strings(String list) {
        List<String> out = new ArrayList<>();
        if (entry.has(list) && entry.get(list).isJsonArray()) {
            for (JsonElement e : entry.getAsJsonArray(list)) out.add(e.getAsString());
        }
        return out;
    }

    List<UUID> uuids(String list) {
        List<UUID> out = new ArrayList<>();
        for (String s : strings(list)) {
            try {
                out.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // A hand-edited ledger line; skip it rather than abort the whole clear.
            }
        }
        return out;
    }

    /* ------------------------------------------ overwritten ----------------------------------------- */

    /** Records the value a fill is about to overwrite, unless an earlier fill already did. */
    void prior(String name, JsonElement value) {
        JsonObject prior = entry.has("prior") ? entry.getAsJsonObject("prior") : new JsonObject();
        if (!prior.has(name)) prior.add(name, value);
        entry.add("prior", prior);
    }

    JsonElement prior(String name) {
        return entry.has("prior") ? entry.getAsJsonObject("prior").get(name) : null;
    }

    /* --------------------------------------------- disk --------------------------------------------- */

    void save() {
        root.add(key, entry);
        MinestarFiles.write(path(), root);
    }

    /** Forgets this player's entry, once clear has undone it. */
    void delete() {
        root.remove(key);
        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
