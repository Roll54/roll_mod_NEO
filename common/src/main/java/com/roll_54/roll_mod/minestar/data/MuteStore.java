package com.roll_54.roll_mod.minestar.data;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who may not speak, and until when.
 *
 * <p>Replaces the mute FTB Essentials used to hold — {@code ChatHandler} reads this now. Kept in
 * {@code minestar/mutes.json} with the rest of the operator-facing data, so a mute can be lifted by
 * editing a file when nobody with the command is online.
 *
 * <p>{@code until = 0} means indefinite; anything else is epoch millis and expires on its own.
 */
public final class MuteStore {

    private static final String FILE = "mutes.json";

    /** One mute. {@code by} and {@code reason} are for whoever reads the file, not for the code. */
    public record Mute(long until, String by, String reason) {

        public boolean expired(long now) {
            return until != 0L && now >= until;
        }

        /** Remaining time in millis, or {@code -1} when the mute never expires. */
        public long remaining(long now) {
            return until == 0L ? -1L : Math.max(0L, until - now);
        }
    }

    private static final Map<UUID, Mute> MUTES = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private MuteStore() {}

    public static boolean isMuted(UUID player) {
        return mute(player) != null;
    }

    /** The player's mute, or {@code null} when they are not muted or the mute has run out. */
    public static @Nullable Mute mute(UUID player) {
        load();
        Mute mute = MUTES.get(player);
        if (mute == null) return null;
        if (mute.expired(System.currentTimeMillis())) {
            unmute(player);
            return null;
        }
        return mute;
    }

    /** Mutes until {@code until} epoch millis; {@code 0} never expires. */
    public static synchronized void mute(UUID player, long until, String by, String reason) {
        load();
        MUTES.put(player, new Mute(until, by, reason));
        save();
    }

    public static synchronized boolean unmute(UUID player) {
        load();
        boolean removed = MUTES.remove(player) != null;
        if (removed) save();
        return removed;
    }

    public static void reload() {
        synchronized (MuteStore.class) {
            MUTES.clear();
            loaded = false;
        }
    }

    /** Convenience for the command layer, which has a player rather than an id. */
    public static boolean isMuted(ServerPlayer player) {
        return isMuted(player.getUUID());
    }

    private static void load() {
        if (loaded) return;
        synchronized (MuteStore.class) {
            if (loaded) return;
            JsonObject root = MinestarFiles.readObject(path());
            for (String key : root.keySet()) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                JsonObject row = root.getAsJsonObject(key);
                MUTES.put(id, new Mute(
                        row.has("until") ? row.get("until").getAsLong() : 0L,
                        row.has("by") ? row.get("by").getAsString() : "",
                        row.has("reason") ? row.get("reason").getAsString() : ""));
            }
            loaded = true;
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        MUTES.forEach((id, mute) -> {
            JsonObject row = new JsonObject();
            row.addProperty("until", mute.until());
            row.addProperty("by", mute.by());
            row.addProperty("reason", mute.reason());
            root.add(id.toString(), row);
        });
        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
