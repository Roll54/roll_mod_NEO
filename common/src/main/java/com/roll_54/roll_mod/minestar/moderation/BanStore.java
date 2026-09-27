package com.roll_54.roll_mod.minestar.moderation;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who may not join, and until when. The ban half of {@code MuteStore}, and deliberately its twin:
 * same file shape, same {@code until = 0} meaning indefinite, same self-expiry on read.
 *
 * <p>Kept in {@code minestar/bans.json} rather than mirrored into vanilla's
 * {@code banned-players.json}. Mirroring would buy {@code PlayerList.canPlayerLogin}'s rejection
 * for free, at the cost of two sources of truth that drift apart the moment someone types
 * {@code /pardon}. One file, one answer.
 *
 * <p>Expiry needs no sweeper: {@link #ban(UUID)} drops a ban that has run out at the moment it is
 * asked about, so a ban ends whether or not anyone was online to notice.
 *
 * <p><b>Read off the server thread.</b> The login gate asks this during connection negotiation,
 * before there is a {@code ServerPlayer} or a level. That is safe here — the map is concurrent and
 * {@link #load()} is a synchronized double-check — and it is why this class touches nothing else.
 */
public final class BanStore {

    private static final String FILE = "bans.json";

    /**
     * One ban. {@code name} is the player's last known name, carried so the moderation tab can list
     * an offline ban without a profile lookup.
     */
    public record Ban(long until, String by, String rule, String reason, String name) {

        public boolean expired(long now) {
            return until != 0L && now >= until;
        }

        /** Remaining time in millis, or {@code -1} when the ban never expires. */
        public long remaining(long now) {
            return until == 0L ? -1L : Math.max(0L, until - now);
        }

        public boolean permanent() {
            return until == 0L;
        }
    }

    private static final Map<UUID, Ban> BANS = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private BanStore() {}

    public static boolean isBanned(UUID player) {
        return ban(player) != null;
    }

    /**
     * The player's ban, or {@code null} when they are not banned or the ban has run out.
     *
     * <p>Tolerates a null id. The login gate asks during connection negotiation, where the profile
     * is whatever the connecting client sent and its id is not guaranteed to be filled in yet — and
     * {@link java.util.concurrent.ConcurrentHashMap#get} throws on null rather than missing.
     */
    public static @Nullable Ban ban(@Nullable UUID player) {
        if (player == null) return null;
        load();
        Ban ban = BANS.get(player);
        if (ban == null) return null;
        if (ban.expired(System.currentTimeMillis())) {
            unban(player);
            return null;
        }
        return ban;
    }

    /** Bans until {@code until} epoch millis; {@code 0} never expires. */
    public static synchronized void ban(UUID player, long until, String by, String rule,
                                        String reason, String name) {
        load();
        BANS.put(player, new Ban(until, by, rule, reason, name));
        save();
    }

    public static synchronized boolean unban(UUID player) {
        load();
        boolean removed = BANS.remove(player) != null;
        if (removed) save();
        return removed;
    }

    /** Every ban still in force, for the moderator's list. Expired entries are dropped as they go. */
    public static Map<UUID, Ban> active() {
        load();
        long now = System.currentTimeMillis();
        Map<UUID, Ban> active = new HashMap<>();
        BANS.forEach((id, ban) -> {
            if (!ban.expired(now)) active.put(id, ban);
        });
        return active;
    }

    public static void reload() {
        ModerationViewers.markDirty();
        synchronized (BanStore.class) {
            BANS.clear();
            loaded = false;
        }
    }

    private static void load() {
        if (loaded) return;
        synchronized (BanStore.class) {
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
                BANS.put(id, new Ban(
                        row.has("until") ? row.get("until").getAsLong() : 0L,
                        row.has("by") ? row.get("by").getAsString() : "",
                        row.has("rule") ? row.get("rule").getAsString() : "",
                        row.has("reason") ? row.get("reason").getAsString() : "",
                        row.has("name") ? row.get("name").getAsString() : ""));
            }
            loaded = true;
        }
    }

    private static void save() {
        ModerationViewers.markDirty();
        JsonObject root = new JsonObject();
        BANS.forEach((id, ban) -> {
            JsonObject row = new JsonObject();
            row.addProperty("until", ban.until());
            row.addProperty("by", ban.by());
            row.addProperty("rule", ban.rule());
            row.addProperty("reason", ban.reason());
            row.addProperty("name", ban.name());
            root.add(id.toString(), row);
        });
        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
