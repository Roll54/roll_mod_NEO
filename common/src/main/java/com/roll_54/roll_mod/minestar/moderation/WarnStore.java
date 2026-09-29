package com.roll_54.roll_mod.minestar.moderation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every warning ever handed out, per player.
 *
 * <p>Kept in {@code minestar/warns.json} beside the mutes, so a warning can be read — or taken back
 * — by editing a file when nobody with the command is online.
 *
 * <p><b>Warnings never expire and never reset.</b> That is a deliberate server rule, not an
 * oversight, and it is enforced by what this class does <em>not</em> have: {@link Warn} carries no
 * {@code expired(now)} and this class carries no sweeper, so there is no code path that could drop
 * one. {@link #count} is simply how many are on file. The third warning, and every warning after
 * it, earns a day's ban — see {@code ModerationService.warn}, which owns that rule; this class only
 * remembers.
 *
 * <p>The player's last known name is stored alongside, so the moderation list can show a warned
 * player who is offline without a profile lookup.
 */
public final class WarnStore {

    private static final String FILE = "warns.json";

    /**
     * One warning. {@code by} is the moderator's name, {@code rule} the cited {@link Rule#id()},
     * {@code note} whatever they typed. All three are for whoever reads the file; the code only
     * counts them.
     */
    public record Warn(long at, String by, String rule, String note) {}

    /** Per player: their last known name, and every warning in the order they were given. */
    private record Record(String name, List<Warn> warns) {}

    private static final Map<UUID, Record> WARNS = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private WarnStore() {}

    /** How many warnings this player has. Never goes down on its own. */
    public static int count(UUID player) {
        load();
        Record record = WARNS.get(player);
        return record == null ? 0 : record.warns().size();
    }

    /** Every warning, oldest first. */
    public static List<Warn> warns(UUID player) {
        load();
        Record record = WARNS.get(player);
        return record == null ? List.of() : List.copyOf(record.warns());
    }

    /** The last name seen for a warned player, or {@code ""}. */
    public static String name(UUID player) {
        load();
        Record record = WARNS.get(player);
        return record == null ? "" : record.name();
    }

    /** Records a warning and returns the player's new total. */
    public static synchronized int add(UUID player, String name, Warn warn) {
        load();
        Record existing = WARNS.get(player);
        List<Warn> warns = existing == null ? new ArrayList<>() : new ArrayList<>(existing.warns());
        warns.add(warn);
        WARNS.put(player, new Record(name, warns));
        save();
        return warns.size();
    }

    /**
     * Wipes a player's warnings. Only the command layer calls this — nothing automatic does, which
     * is the whole point of the never-resets rule. Returns how many were cleared.
     */
    public static synchronized int clear(UUID player) {
        load();
        Record removed = WARNS.remove(player);
        if (removed == null) return 0;
        save();
        return removed.warns().size();
    }

    /**
     * Drops only the warnings {@code filter} matches, keeping the rest. For tooling that must undo
     * its own warnings without touching real ones — {@link #clear} would take those too. Returns how
     * many were removed.
     */
    public static synchronized int removeWhere(UUID player, java.util.function.Predicate<Warn> filter) {
        load();
        Record existing = WARNS.get(player);
        if (existing == null) return 0;
        List<Warn> kept = new ArrayList<>();
        for (Warn warn : existing.warns()) {
            if (!filter.test(warn)) kept.add(warn);
        }
        int removed = existing.warns().size() - kept.size();
        if (removed == 0) return 0;
        if (kept.isEmpty()) {
            WARNS.remove(player);
        } else {
            WARNS.put(player, new Record(existing.name(), kept));
        }
        save();
        return removed;
    }

    /** Everyone with at least one warning, for the moderator's list. */
    public static Map<UUID, Integer> counts() {
        load();
        Map<UUID, Integer> counts = new java.util.HashMap<>();
        WARNS.forEach((id, record) -> counts.put(id, record.warns().size()));
        return counts;
    }

    public static void reload() {
        ModerationViewers.markDirty();
        synchronized (WarnStore.class) {
            WARNS.clear();
            loaded = false;
        }
    }

    private static void load() {
        if (loaded) return;
        synchronized (WarnStore.class) {
            if (loaded) return;
            JsonObject root = MinestarFiles.readObject(path());
            for (String key : root.keySet()) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (!root.get(key).isJsonObject()) continue;
                JsonObject row = root.getAsJsonObject(key);

                List<Warn> warns = new ArrayList<>();
                if (row.has("warns") && row.get("warns").isJsonArray()) {
                    JsonArray array = row.getAsJsonArray("warns");
                    for (JsonElement element : array) {
                        if (!element.isJsonObject()) continue;
                        JsonObject entry = element.getAsJsonObject();
                        warns.add(new Warn(
                                entry.has("at") ? entry.get("at").getAsLong() : 0L,
                                entry.has("by") ? entry.get("by").getAsString() : "",
                                entry.has("rule") ? entry.get("rule").getAsString() : "",
                                entry.has("note") ? entry.get("note").getAsString() : ""));
                    }
                }
                // A player with an empty list is the same as a player with no entry; dropping it
                // keeps a hand-edited file from accumulating husks.
                if (warns.isEmpty()) continue;
                WARNS.put(id, new Record(
                        row.has("name") ? row.get("name").getAsString() : "", warns));
            }
            loaded = true;
        }
    }

    private static void save() {
        ModerationViewers.markDirty();
        JsonObject root = new JsonObject();
        WARNS.forEach((id, record) -> {
            JsonArray warns = new JsonArray();
            for (Warn warn : record.warns()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("at", warn.at());
                entry.addProperty("by", warn.by());
                entry.addProperty("rule", warn.rule());
                entry.addProperty("note", warn.note());
                warns.add(entry);
            }
            JsonObject row = new JsonObject();
            row.addProperty("name", record.name());
            row.add("warns", warns);
            root.add(id.toString(), row);
        });
        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
