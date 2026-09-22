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

    private record Entry(@Nullable Spot back, long rtpLastUsed) {}

    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

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

    /** Records where the player is standing now, to be called <em>before</em> moving them. */
    public static void rememberBack(ServerPlayer player) {
        put(player.getUUID(), new Entry(Spot.of(player), rtpLastUsed(player.getUUID())));
    }

    /** Forgets the stored position, so a failed {@code /back} does not keep offering it. */
    public static void clearBack(UUID player) {
        put(player, new Entry(null, rtpLastUsed(player)));
    }

    public static void rtpUsed(UUID player, long epochMillis) {
        put(player, new Entry(back(player), epochMillis));
    }

    /* ------------------------------------------ persistence ----------------------------------------- */

    /** Drops the cache so the next read picks the file up again, for {@code /rollmod reload}. */
    public static synchronized void reload() {
        ENTRIES.clear();
        loaded = false;
    }

    private static Entry entry(UUID player) {
        load();
        return ENTRIES.getOrDefault(player, new Entry(null, 0L));
    }

    private static synchronized void put(UUID player, Entry entry) {
        load();
        ENTRIES.put(player, entry);
        save();
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
                Spot back = row.has("back") ? Spot.load(row.getAsJsonObject("back")) : null;
                long rtp = row.has("rtpLastUsed") ? row.get("rtpLastUsed").getAsLong() : 0L;
                ENTRIES.put(id, new Entry(back, rtp));
            }
            loaded = true;
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        ENTRIES.forEach((id, entry) -> {
            if (entry.back() == null && entry.rtpLastUsed() == 0L) return;
            JsonObject row = new JsonObject();
            if (entry.back() != null) row.add("back", entry.back().save());
            if (entry.rtpLastUsed() != 0L) row.addProperty("rtpLastUsed", entry.rtpLastUsed());
            root.add(id.toString(), row);
        });
        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
