package com.roll_54.roll_mod.minestar.tpa;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.compat.ftb.TeamsFacade;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each player's answer to "who may ask me for a teleport" — {@code minestar/tpa.json}.
 *
 * <p>A file of its own rather than a field in {@code teleports.json}: that one is rewritten on every
 * teleport and every death, and a setting a player picks once does not belong in a hot path. Same
 * shape as {@code MuteStore}.
 *
 * <p>A row at the default is dropped rather than written, so the file lists only the people who
 * have actually changed something.
 */
public final class TpaSettings {

    private static final String FILE = "tpa.json";

    private static final Map<UUID, TpaMode> MODES = new ConcurrentHashMap<>();

    /** Names, written for whoever opens the file and never read back. */
    private static final Map<UUID, String> NAMES = new ConcurrentHashMap<>();

    private static volatile boolean loaded;

    private TpaSettings() {}

    public static TpaMode mode(UUID player) {
        load();
        return MODES.getOrDefault(player, TpaMode.DEFAULT);
    }

    public static synchronized void set(ServerPlayer player, TpaMode mode) {
        load();
        if (mode == TpaMode.DEFAULT) {
            MODES.remove(player.getUUID());
            NAMES.remove(player.getUUID());
        } else {
            MODES.put(player.getUUID(), mode);
            NAMES.put(player.getUUID(), player.getGameProfile().getName());
        }
        save();
    }

    /**
     * Whether {@code from} may ask {@code target} for a teleport.
     *
     * <p>Asked of the target's setting, in the target's direction: an alliance is a thing one team
     * extends to another, so "is the sender an ally" is a question only the recipient's team can
     * answer.
     */
    public static boolean allows(ServerPlayer target, ServerPlayer from) {
        return switch (mode(target.getUUID())) {
            case EVERYONE -> true;
            case NOBODY -> false;
            case TEAM_ONLY -> TeamsFacade.get().sameParty(target, from.getUUID());
            case TEAM_AND_ALLIES -> TeamsFacade.get().trusted(target, from.getUUID());
        };
    }

    /** Drops the cache so the next read picks the file up again, for {@code /rollmod reload}. */
    public static synchronized void reload() {
        MODES.clear();
        NAMES.clear();
        loaded = false;
    }

    private static void load() {
        if (loaded) return;
        synchronized (TpaSettings.class) {
            if (loaded) return;
            JsonObject root = MinestarFiles.readObject(path());
            for (String key : root.keySet()) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue; // a hand-written key that is not a uuid: skip the row, keep the rest
                }
                JsonObject row = root.getAsJsonObject(key);
                TpaMode mode = TpaMode.byId(row.has("allow") ? row.get("allow").getAsString() : null);
                if (mode == TpaMode.DEFAULT) continue;
                MODES.put(id, mode);
                if (row.has("name")) NAMES.put(id, row.get("name").getAsString());
            }
            loaded = true;
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        MODES.forEach((id, mode) -> {
            JsonObject row = new JsonObject();
            row.addProperty("allow", mode.id());
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
