package com.roll_54.roll_mod.minestar.kits;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When each player last claimed each kit — {@code minestar/kits/cooldowns.json}.
 *
 * <p>Epoch millis, not tick counts: a tick count restarts with the server, which would hand
 * everybody a free round of kits after every reboot.
 */
public final class KitCooldowns {

    private static final String FILE = "cooldowns.json";

    private static final Map<UUID, Map<String, Long>> CLAIMS = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private KitCooldowns() {}

    /** Epoch millis of the last claim, or {@code 0} when the player has never claimed it. */
    public static long lastClaimed(UUID player, String kit) {
        load();
        return CLAIMS.getOrDefault(player, Map.of()).getOrDefault(kit, 0L);
    }

    public static synchronized void claimed(UUID player, String kit, long epochMillis) {
        load();
        CLAIMS.computeIfAbsent(player, id -> new ConcurrentHashMap<>()).put(kit, epochMillis);
        save();
    }

    /** Forgets every stamp for one kit, for when the kit is deleted or recreated. */
    public static synchronized void forget(String kit) {
        load();
        boolean changed = false;
        for (Map<String, Long> byKit : CLAIMS.values()) {
            changed |= byKit.remove(kit) != null;
        }
        if (changed) save();
    }

    public static void reload() {
        synchronized (KitCooldowns.class) {
            CLAIMS.clear();
            loaded = false;
        }
    }

    private static void load() {
        if (loaded) return;
        synchronized (KitCooldowns.class) {
            if (loaded) return;
            JsonObject root = MinestarFiles.readObject(path());
            for (String key : root.keySet()) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                Map<String, Long> byKit = new ConcurrentHashMap<>();
                JsonObject row = root.getAsJsonObject(key);
                for (String kit : row.keySet()) {
                    byKit.put(kit, row.get(kit).getAsLong());
                }
                CLAIMS.put(id, byKit);
            }
            loaded = true;
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        CLAIMS.forEach((id, byKit) -> {
            if (byKit.isEmpty()) return;
            JsonObject row = new JsonObject();
            byKit.forEach(row::addProperty);
            root.add(id.toString(), row);
        });
        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve("kits", FILE);
    }
}
