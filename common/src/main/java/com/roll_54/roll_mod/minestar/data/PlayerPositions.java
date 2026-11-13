package com.roll_54.roll_mod.minestar.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where a player was before the last thing that moved them, and when they last used {@code /rtp}.
 *
 * <p>Lives in {@code minestar/teleports.json} rather than in player NBT so an operator can read it,
 * clear a stuck entry, or hand a player their spot back after a mishap.
 *
 * <p>The back position is written by every teleport this mod performs and on death — see
 * {@link com.roll_54.roll_mod.minestar.teleport.TeleportEvents}. One position, not a history: the
 * second {@code /back} in a row returns to where the first one started, which is what players
 * expect from it.
 */
public final class PlayerPositions {

    private static final String FILE = "teleports.json";

    /** A place to return to. Dimension is stored by id, so an unloaded dimension just fails to resolve. */
    public record Spot(ResourceLocation dimension, double x, double y, double z, float yaw, float pitch) {

        public static Spot of(ServerPlayer player) {
            return new Spot(player.level().dimension().location(), player.getX(), player.getY(),
                    player.getZ(), player.getYRot(), player.getXRot());
        }

        public @Nullable ServerLevel level(MinecraftServer server) {
            return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        }

        JsonObject save() {
            JsonObject json = new JsonObject();
            json.addProperty("dim", dimension.toString());
            json.addProperty("x", x);
            json.addProperty("y", y);
            json.addProperty("z", z);
            json.addProperty("yaw", yaw);
            json.addProperty("pitch", pitch);
            return json;
        }

        static @Nullable Spot load(JsonObject json) {
            if (!json.has("dim")) return null;
            ResourceLocation dim = ResourceLocation.tryParse(json.get("dim").getAsString());
            if (dim == null) return null;
            return new Spot(dim, json.get("x").getAsDouble(), json.get("y").getAsDouble(),
                    json.get("z").getAsDouble(),
                    json.has("yaw") ? json.get("yaw").getAsFloat() : 0f,
                    json.has("pitch") ? json.get("pitch").getAsFloat() : 0f);
        }
    }

    /**
     * {@code history}: where the player was before each teleport (and where they died), oldest first, as FTB
     * Essentials keeps it — {@code /back} takes the newest and drops it, so repeated uses walk further back.
     */
    private record Entry(List<Spot> history, long rtpLastUsed) {
        @Nullable Spot back() {
            return history.isEmpty() ? null : history.get(history.size() - 1);
        }
    }

    /**
     * How often pending changes reach the disk, in ticks.
     *
     * <p>Every teleport in the mod writes a back position, and rewriting a file that holds a row
     * per player ever seen, on the server thread, per teleport, is a stutter players pay for other
     * players' commands. So writes batch: at worst this many ticks of {@code /back} history are
     * lost to a crash, which {@code /back} — a convenience, not an inventory — can afford.
     */
    private static final int SAVE_INTERVAL_TICKS = 200;

    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static volatile boolean loaded;
    private static volatile boolean dirty;

    private PlayerPositions() {}

    /* -------------------------------------------- reading ------------------------------------------- */

    public static @Nullable Spot back(UUID player) {
        return entry(player).back();
    }

    /** Epoch millis of the player's last random teleport, or {@code 0} when they have never used one. */
    public static long rtpLastUsed(UUID player) {
        return entry(player).rtpLastUsed();
    }

    /* -------------------------------------------- writing ------------------------------------------- */

    /** Records where the player is standing now, to be called <em>before</em> moving them; keeps the newest {@code max}. */
    public static void rememberBack(ServerPlayer player) {
        Entry entry = entry(player.getUUID());
        List<Spot> history = new ArrayList<>(entry.history());
        history.add(Spot.of(player));
        int max = Math.max(1, com.roll_54.roll_mod.config.MyConfig.INSTANCE == null ? 10
                : com.roll_54.roll_mod.config.MyConfig.INSTANCE.back.historySize);
        while (history.size() > max) history.remove(0);
        put(player.getUUID(), new Entry(List.copyOf(history), entry.rtpLastUsed()));
    }

    /** Drops the newest position: used by {@code /back}, or because it can no longer be reached. */
    public static void popBack(UUID player) {
        Entry entry = entry(player);
        if (entry.history().isEmpty()) return;
        put(player, new Entry(List.copyOf(entry.history().subList(0, entry.history().size() - 1)), entry.rtpLastUsed()));
    }

    /** The newest position only, as before. Kept for callers that forgot a spot that failed. */
    public static void clearBack(UUID player) {
        popBack(player);
    }

    public static void rtpUsed(UUID player, long epochMillis) {
        put(player, new Entry(entry(player).history(), epochMillis));
    }

    /* ------------------------------------------ persistence ----------------------------------------- */

    /**
     * Drops the cache so the next read picks the file up again, for {@code /rollmod reload}.
     *
     * <p>Unsaved changes are dropped with it, deliberately: reload exists to pick up an operator's
     * hand edit, and flushing first would overwrite the very edit being loaded.
     */
    public static synchronized void reload() {
        ENTRIES.clear();
        loaded = false;
        dirty = false;
    }

    /** Batches the disk writes. Called once a tick; see {@link #SAVE_INTERVAL_TICKS}. */
    public static void tick(MinecraftServer server) {
        if (dirty && server.getTickCount() % SAVE_INTERVAL_TICKS == 0) flush();
    }

    /** Writes pending changes now, for server shutdown. */
    public static synchronized void flush() {
        if (!dirty) return;
        dirty = false;
        save();
    }

    private static Entry entry(UUID player) {
        load();
        return ENTRIES.getOrDefault(player, new Entry(List.of(), 0L));
    }

    private static synchronized void put(UUID player, Entry entry) {
        load();
        ENTRIES.put(player, entry);
        dirty = true;
    }

    private static void load() {
        if (loaded) return;
        synchronized (PlayerPositions.class) {
            if (loaded) return;
            JsonObject root = MinestarFiles.readObject(path());
            for (String key : root.keySet()) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue; // hand-edited nonsense: skip the row, keep the rest
                }
                JsonObject row = root.getAsJsonObject(key);
                List<Spot> history = new ArrayList<>();
                if (row.has("history") && row.get("history").isJsonArray()) {
                    for (var element : row.getAsJsonArray("history")) {
                        Spot spot = element.isJsonObject() ? Spot.load(element.getAsJsonObject()) : null;
                        if (spot != null) history.add(spot);
                    }
                } else if (row.has("back")) {
                    // An older file: its one back position starts the history.
                    Spot back = Spot.load(row.getAsJsonObject("back"));
                    if (back != null) history.add(back);
                }
                long rtp = row.has("rtpLastUsed") ? row.get("rtpLastUsed").getAsLong() : 0L;
                ENTRIES.put(id, new Entry(List.copyOf(history), rtp));
            }
            loaded = true;
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        ENTRIES.forEach((id, entry) -> {
            if (entry.history().isEmpty() && entry.rtpLastUsed() == 0L) return;
            JsonObject row = new JsonObject();
            if (!entry.history().isEmpty()) {
                com.google.gson.JsonArray history = new com.google.gson.JsonArray();
                entry.history().forEach(spot -> history.add(spot.save()));
                row.add("history", history);
            }
            if (entry.rtpLastUsed() != 0L) row.addProperty("rtpLastUsed", entry.rtpLastUsed());
            root.add(id.toString(), row);
        });
        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
