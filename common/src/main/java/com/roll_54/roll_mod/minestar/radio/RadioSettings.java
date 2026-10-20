package com.roll_54.roll_mod.minestar.radio;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each radio implant owner's tuning — {@code minestar/radio.json}: the station, whether they hear it,
 * and whether the players around them do. Same shape as {@code TpaSettings}; a row at the default is
 * dropped rather than written.
 */
public final class RadioSettings {

    private static final String FILE = "radio.json";

    /** {@code station} is a station url from Ad Astra's list, or empty for none. */
    public record Setting(String station, boolean hear, boolean broadcast) {
        public static final Setting DEFAULT = new Setting("", true, false);
    }

    private static final Map<UUID, Setting> SETTINGS = new ConcurrentHashMap<>();
    /** Names, written for whoever opens the file and never read back. */
    private static final Map<UUID, String> NAMES = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private RadioSettings() {}

    public static Setting get(UUID player) {
        load();
        return SETTINGS.getOrDefault(player, Setting.DEFAULT);
    }

    public static synchronized void set(ServerPlayer player, Setting setting) {
        load();
        if (setting.equals(Setting.DEFAULT)) {
            SETTINGS.remove(player.getUUID());
            NAMES.remove(player.getUUID());
        } else {
            SETTINGS.put(player.getUUID(), setting);
            NAMES.put(player.getUUID(), player.getGameProfile().getName());
        }
        save();
    }

    /** Drops the cache so the next read picks the file up again, for {@code /rollmod reload}. */
    public static synchronized void reload() {
        SETTINGS.clear();
        NAMES.clear();
        loaded = false;
    }

    private static void load() {
        if (loaded) return;
        synchronized (RadioSettings.class) {
            if (loaded) return;
            JsonObject root = MinestarFiles.readObject(path());
            for (String key : root.keySet()) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue; // a hand-written key that is not a uuid: skip the row, keep the rest
                }
                if (!root.get(key).isJsonObject()) continue;
                JsonObject row = root.getAsJsonObject(key);
                SETTINGS.put(id, new Setting(
                        row.has("station") ? row.get("station").getAsString() : "",
                        !row.has("hear") || row.get("hear").getAsBoolean(),
                        row.has("broadcast") && row.get("broadcast").getAsBoolean()));
                if (row.has("name")) NAMES.put(id, row.get("name").getAsString());
            }
            loaded = true;
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        SETTINGS.forEach((id, s) -> {
            JsonObject row = new JsonObject();
            row.addProperty("station", s.station());
            row.addProperty("hear", s.hear());
            row.addProperty("broadcast", s.broadcast());
            String name = NAMES.get(id);
            if (name != null) row.addProperty("name", name);
            root.add(id.toString(), row);
        });
        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
