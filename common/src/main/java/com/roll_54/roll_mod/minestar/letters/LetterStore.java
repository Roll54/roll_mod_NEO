package com.roll_54.roll_mod.minestar.letters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every letter, in {@code minestar/letters.json}, newest first.
 *
 * <p>Takes a {@link MinecraftServer} on every public method — the {@code KitStore} shape — because
 * item rewards need the registries to read and write.
 *
 * <p>Expired letters are kept, so staff can look back at them and re-open or re-send one, but only
 * for {@link #KEEP_EXPIRED} — {@code accepted} grows by one entry per reader per letter, and without
 * a limit the file would only ever grow.
 */
public final class LetterStore {

    private static final String FILE = "letters.json";

    /** How long an expired letter is kept as history before it is pruned on load. */
    private static final long KEEP_EXPIRED = 30L * 24L * 60L * 60L * 1000L;

    private static final Map<UUID, Letter> LETTERS = new LinkedHashMap<>();
    private static boolean loaded;

    private LetterStore() {}

    /** Every letter still visible, newest first. */
    public static synchronized List<Letter> visible(MinecraftServer server) {
        load(server.registryAccess());
        long now = System.currentTimeMillis();
        List<Letter> out = new ArrayList<>();
        for (Letter letter : LETTERS.values()) {
            if (letter.visible(now)) out.add(letter);
        }
        out.sort((a, b) -> Long.compare(b.createdAt(), a.createdAt()));
        return out;
    }

    /** Every letter {@code reader} may read right now — broadcasts and their own — newest first. */
    public static synchronized List<Letter> visibleTo(MinecraftServer server, UUID reader) {
        List<Letter> out = new ArrayList<>();
        for (Letter letter : visible(server)) {
            if (letter.isFor(reader)) out.add(letter);
        }
        return out;
    }

    /** Every letter, due and expired included, newest first — the staff list. */
    public static synchronized List<Letter> all(MinecraftServer server) {
        load(server.registryAccess());
        List<Letter> out = new ArrayList<>(LETTERS.values());
        out.sort((a, b) -> Long.compare(b.createdAt(), a.createdAt()));
        return out;
    }

    public static synchronized @Nullable Letter byId(MinecraftServer server, UUID id) {
        load(server.registryAccess());
        return LETTERS.get(id);
    }

    public static synchronized void put(MinecraftServer server, Letter letter) {
        load(server.registryAccess());
        LETTERS.put(letter.id(), letter);
        save(server.registryAccess());
    }

    public static synchronized boolean remove(MinecraftServer server, UUID id) {
        load(server.registryAccess());
        if (LETTERS.remove(id) == null) return false;
        save(server.registryAccess());
        return true;
    }

    /**
     * Records that {@code reader} took this letter. {@code false} when they already had, which is
     * what stops a double click paying out twice — the caller grants rewards only on {@code true}.
     */
    public static synchronized boolean markAccepted(MinecraftServer server, UUID id, UUID reader) {
        load(server.registryAccess());
        Letter letter = LETTERS.get(id);
        if (letter == null || letter.acceptedBy(reader)) return false;
        Map<UUID, Long> accepted = new HashMap<>(letter.accepted());
        accepted.put(reader, System.currentTimeMillis());
        LETTERS.put(id, new Letter(letter.id(), letter.title(), letter.body(), letter.author(),
                letter.createdAt(), letter.sendAt(), letter.expiresAt(), letter.rewards(), letter.icon(),
                Map.copyOf(accepted), letter.recipient()));
        save(server.registryAccess());
        return true;
    }

    public static synchronized void reload() {
        LETTERS.clear();
        loaded = false;
    }

    /* ---------------------------------------------- io ---------------------------------------------- */

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }

    private static void load(RegistryAccess registries) {
        if (loaded) return;
        loaded = true;
        LETTERS.clear();

        JsonObject root = MinestarFiles.readObject(path());
        if (!root.has("letters") || !root.get("letters").isJsonArray()) return;

        long now = System.currentTimeMillis();
        int dropped = 0;
        for (JsonElement element : root.getAsJsonArray("letters")) {
            try {
                Letter letter = read(element.getAsJsonObject(), registries);
                if (letter == null) continue;
                if (letter.expiresAt() != 0L && now - letter.expiresAt() > KEEP_EXPIRED) {
                    dropped++;
                    continue;
                }
                LETTERS.put(letter.id(), letter);
            } catch (RuntimeException e) {
                RollMod.LOGGER.error("[Letters] skipped a malformed letter in {}", path(), e);
            }
        }
        if (dropped > 0) save(registries);
    }

    private static @Nullable Letter read(JsonObject json, RegistryAccess registries) {
        if (!json.has("id")) return null;
        UUID id = UUID.fromString(json.get("id").getAsString());

        List<LetterReward> rewards = new ArrayList<>();
        if (json.has("rewards") && json.get("rewards").isJsonArray()) {
            for (JsonElement r : json.getAsJsonArray("rewards")) {
                if (!r.isJsonObject()) continue;
                LetterReward reward = LetterReward.load(r.getAsJsonObject(), registries);
                if (reward != null) rewards.add(reward);
            }
        }

        Map<UUID, Long> accepted = new HashMap<>();
        if (json.has("accepted") && json.get("accepted").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("accepted").entrySet()) {
                try {
                    accepted.put(UUID.fromString(e.getKey()), e.getValue().getAsLong());
                } catch (RuntimeException ignored) {
                    // One unreadable reader entry costs that reader a re-offer, nothing more.
                }
            }
        }

        return new Letter(id,
                string(json, "title"), string(json, "body"), string(json, "author"),
                json.has("createdAt") ? json.get("createdAt").getAsLong() : 0L,
                json.has("sendAt") ? json.get("sendAt").getAsLong() : 0L,
                json.has("expiresAt") ? json.get("expiresAt").getAsLong() : 0L,
                List.copyOf(rewards), string(json, "icon"), Map.copyOf(accepted),
                // Absent on every broadcast, and on every letter written before personal ones existed.
                json.has("recipient") ? UUID.fromString(json.get("recipient").getAsString()) : null);
    }

    private static String string(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    private static void save(RegistryAccess registries) {
        JsonArray array = new JsonArray();
        for (Letter letter : LETTERS.values()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", letter.id().toString());
            json.addProperty("title", letter.title());
            json.addProperty("body", letter.body());
            json.addProperty("author", letter.author());
            json.addProperty("createdAt", letter.createdAt());
            json.addProperty("sendAt", letter.sendAt());
            json.addProperty("expiresAt", letter.expiresAt());
            if (!letter.icon().isEmpty()) json.addProperty("icon", letter.icon());
            if (letter.recipient() != null) json.addProperty("recipient", letter.recipient().toString());

            JsonArray rewards = new JsonArray();
            for (LetterReward reward : letter.rewards()) {
                JsonObject r = reward.save(registries);
                if (r != null) rewards.add(r);
            }
            json.add("rewards", rewards);

            JsonObject accepted = new JsonObject();
            letter.accepted().forEach((reader, when) -> accepted.addProperty(reader.toString(), when));
            json.add("accepted", accepted);
            array.add(json);
        }
        JsonObject root = new JsonObject();
        root.add("letters", array);
        MinestarFiles.write(path(), root);
    }
}
