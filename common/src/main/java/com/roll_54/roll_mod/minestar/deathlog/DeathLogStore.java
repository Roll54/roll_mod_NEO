package com.roll_54.roll_mod.minestar.deathlog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The death log on disk: {@code minestar/deathlog/<uuid>.json}, one file per player, newest death
 * first, at most {@code deathlog.maxPerPlayer} of them.
 *
 * <p>A file per player rather than one big file: a death writes one small file, and a moderator
 * looking through one player's deaths reads only that one. The moderation tab's player list needs
 * names and counts without opening every file, so {@code index.json} beside them keeps exactly that.
 *
 * <p>Files are read on first use and kept; {@code /rollmod reload} drops the cache.
 */
public final class DeathLogStore {

    private static final String FOLDER = "deathlog";
    private static final String INDEX = "index";

    /** A player with deaths on record, as the moderation tab lists them. */
    public record Victim(UUID id, String name, int deaths, long lastDeath) {}

    private static final Map<UUID, List<DeathRecord>> CACHE = new HashMap<>();
    private static final Map<UUID, Victim> VICTIMS = new LinkedHashMap<>();
    private static boolean indexLoaded;

    private DeathLogStore() {}

    /** Records a death, newest first, trimming the player's history to the configured length. */
    public static synchronized void add(MinecraftServer server, DeathRecord record) {
        List<DeathRecord> list = new ArrayList<>(deaths(server, record.victim()));
        list.addFirst(record);
        int max = Math.max(1, MyConfig.INSTANCE.deathlog.maxPerPlayer.get());
        while (list.size() > max) list.removeLast();
        write(server, record.victim(), record.victimName(), list);
    }

    /** Swaps in an updated record (a restore noted on it). {@code false} if it is no longer on file. */
    public static synchronized boolean replace(MinecraftServer server, DeathRecord record) {
        List<DeathRecord> list = new ArrayList<>(deaths(server, record.victim()));
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(record.id())) {
                list.set(i, record);
                write(server, record.victim(), record.victimName(), list);
                return true;
            }
        }
        return false;
    }

    /** A player's deaths, newest first. */
    public static synchronized List<DeathRecord> deaths(MinecraftServer server, UUID victim) {
        List<DeathRecord> cached = CACHE.get(victim);
        if (cached != null) return cached;

        JsonObject json = MinestarFiles.readObject(path(victim.toString()));
        List<DeathRecord> list = new ArrayList<>();
        if (json.has("deaths") && json.get("deaths").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("deaths")) {
                if (!e.isJsonObject()) continue;
                DeathRecord record = DeathRecord.load(e.getAsJsonObject(), server.registryAccess());
                if (record != null) list.add(record);
            }
        }
        List<DeathRecord> result = List.copyOf(list);
        CACHE.put(victim, result);
        return result;
    }

    public static synchronized @Nullable DeathRecord find(MinecraftServer server, UUID victim, UUID id) {
        for (DeathRecord record : deaths(server, victim)) {
            if (record.id().equals(id)) return record;
        }
        return null;
    }

    /** Every player with a death on record, most recent death first. */
    public static synchronized List<Victim> victims() {
        loadIndex();
        List<Victim> out = new ArrayList<>(VICTIMS.values());
        out.sort((a, b) -> Long.compare(b.lastDeath(), a.lastDeath()));
        return out;
    }

    public static synchronized void reload() {
        CACHE.clear();
        VICTIMS.clear();
        indexLoaded = false;
    }

    private static void write(MinecraftServer server, UUID victim, String name, List<DeathRecord> list) {
        List<DeathRecord> copy = List.copyOf(list);
        CACHE.put(victim, copy);

        JsonArray array = new JsonArray();
        for (DeathRecord record : copy) array.add(record.save(server.registryAccess()));
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.add("deaths", array);
        MinestarFiles.write(path(victim.toString()), json);

        loadIndex();
        VICTIMS.put(victim, new Victim(victim, name, copy.size(), copy.isEmpty() ? 0L : copy.getFirst().diedAt()));
        saveIndex();
    }

    /**
     * Reads {@code index.json}; if it is missing (first run, or deleted by hand) it is rebuilt from
     * the player files, which is the slow way but only happens once.
     */
    private static void loadIndex() {
        if (indexLoaded) return;
        VICTIMS.clear();
        Path file = path(INDEX);
        if (Files.exists(file)) {
            JsonObject json = MinestarFiles.readObject(file);
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                try {
                    UUID id = UUID.fromString(entry.getKey());
                    JsonObject v = entry.getValue().getAsJsonObject();
                    VICTIMS.put(id, new Victim(id, v.get("name").getAsString(), v.get("deaths").getAsInt(),
                            v.get("lastDeath").getAsLong()));
                } catch (Exception ignored) {
                    // A hand-edited line gone wrong costs that one player's row, not the index.
                }
            }
        } else {
            rebuildIndex();
        }
        indexLoaded = true;
    }

    private static void rebuildIndex() {
        Path dir = MinestarFiles.folder(FOLDER);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                String stem = file.getFileName().toString();
                stem = stem.substring(0, stem.length() - ".json".length());
                if (stem.equals(INDEX)) continue;
                UUID id;
                try {
                    id = UUID.fromString(stem);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                JsonObject json = MinestarFiles.readObject(file);
                JsonArray deaths = json.has("deaths") && json.get("deaths").isJsonArray()
                        ? json.getAsJsonArray("deaths") : new JsonArray();
                if (deaths.isEmpty()) continue;
                long last = 0L;
                try {
                    last = deaths.get(0).getAsJsonObject().get("diedAt").getAsLong();
                } catch (Exception ignored) {
                    // Sorts last; harmless.
                }
                String name = json.has("name") ? json.get("name").getAsString() : stem;
                VICTIMS.put(id, new Victim(id, name, deaths.size(), last));
            }
        } catch (IOException e) {
            RollMod.LOGGER.error("[DeathLog] could not list {}", dir, e);
        }
        saveIndex();
    }

    private static void saveIndex() {
        JsonObject json = new JsonObject();
        for (Victim v : VICTIMS.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", v.name());
            entry.addProperty("deaths", v.deaths());
            entry.addProperty("lastDeath", v.lastDeath());
            json.add(v.id().toString(), entry);
        }
        MinestarFiles.write(path(INDEX), json);
    }

    private static Path path(String name) {
        return MinestarFiles.resolve(FOLDER, name + ".json");
    }
}
