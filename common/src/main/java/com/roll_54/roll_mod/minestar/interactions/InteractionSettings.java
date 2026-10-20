package com.roll_54.roll_mod.minestar.interactions;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import com.roll_54.roll_mod.network.packet.SyncInteractionSettingsPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What each player has set for each other player in the interactions tab's "configure interaction"
 * window — {@code minestar/interactions.json}.
 *
 * <p>Keyed viewer → target: the settings are the viewer's answer to the target, so only the viewer
 * can change them and only the viewer's own decisions read them (is the incoming request from someone
 * they ignore, or someone they always let through). A pair at the default is dropped rather than
 * written, the {@code TpaSettings} way, so the file lists only real choices.
 */
public final class InteractionSettings {

    /** One switch of the window. Append only: the packet sends the ordinal. */
    public enum Flag { AUTO_MONEY, IGNORE, AUTO_TPA, AUTO_TPAHERE }

    /**
     * @param autoMoney   their money offers are accepted at once. Not their requests to be paid —
     *                    those always ask
     * @param ignore      their chat, teleport, money and friend requests do not reach the viewer
     * @param autoTpa     their request to teleport to the viewer is accepted at once
     * @param autoTpaHere their request for the viewer to come to them is accepted at once
     */
    public record Flags(boolean autoMoney, boolean ignore, boolean autoTpa, boolean autoTpaHere) {
        public static final Flags NONE = new Flags(false, false, false, false);

        public boolean get(Flag flag) {
            return switch (flag) {
                case AUTO_MONEY -> autoMoney;
                case IGNORE -> ignore;
                case AUTO_TPA -> autoTpa;
                case AUTO_TPAHERE -> autoTpaHere;
            };
        }

        public Flags with(Flag flag, boolean on) {
            return new Flags(flag == Flag.AUTO_MONEY ? on : autoMoney, flag == Flag.IGNORE ? on : ignore,
                    flag == Flag.AUTO_TPA ? on : autoTpa, flag == Flag.AUTO_TPAHERE ? on : autoTpaHere);
        }

        public boolean isDefault() {
            return equals(NONE);
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeByte((autoMoney ? 1 : 0) | (ignore ? 2 : 0) | (autoTpa ? 4 : 0) | (autoTpaHere ? 8 : 0));
        }

        public static Flags decode(FriendlyByteBuf buf) {
            int bits = buf.readByte();
            return new Flags((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0);
        }
    }

    private static final String FILE = "interactions.json";

    private static final Map<UUID, Map<UUID, Flags>> SETTINGS = new ConcurrentHashMap<>();

    private static volatile boolean loaded;

    private InteractionSettings() {}

    public static Flags flags(UUID viewer, UUID target) {
        load();
        Map<UUID, Flags> row = SETTINGS.get(viewer);
        return row == null ? Flags.NONE : row.getOrDefault(target, Flags.NONE);
    }

    /** Whether {@code viewer} has chosen not to hear from {@code target}. */
    public static boolean ignores(UUID viewer, UUID target) {
        return flags(viewer, target).ignore();
    }

    /** Everything {@code viewer} has set, by target. */
    public static Map<UUID, Flags> all(UUID viewer) {
        load();
        Map<UUID, Flags> row = SETTINGS.get(viewer);
        return row == null ? Map.of() : Map.copyOf(row);
    }

    /** Sets one switch, writes the file, and sends the viewer their new settings. */
    public static synchronized void set(ServerPlayer viewer, UUID target, Flag flag, boolean on) {
        load();
        if (viewer.getUUID().equals(target)) return;
        Map<UUID, Flags> row = SETTINGS.computeIfAbsent(viewer.getUUID(), k -> new ConcurrentHashMap<>());
        Flags next = row.getOrDefault(target, Flags.NONE).with(flag, on);
        if (next.isDefault()) row.remove(target); else row.put(target, next);
        if (row.isEmpty()) SETTINGS.remove(viewer.getUUID());
        save();
        syncTo(viewer);
    }

    public static void syncTo(ServerPlayer viewer) {
        PacketDistributor.sendToPlayer(viewer, new SyncInteractionSettingsPacket(all(viewer.getUUID())));
    }

    /** Drops the cache so the next read picks the file up again, for {@code /rollmod reload}. */
    public static synchronized void reload() {
        SETTINGS.clear();
        loaded = false;
    }

    private static void load() {
        if (loaded) return;
        synchronized (InteractionSettings.class) {
            if (loaded) return;
            JsonObject root = MinestarFiles.readObject(path());
            for (String viewerKey : root.keySet()) {
                UUID viewer = uuid(viewerKey);
                if (viewer == null || !root.get(viewerKey).isJsonObject()) continue;
                JsonObject targets = root.getAsJsonObject(viewerKey);
                Map<UUID, Flags> row = new ConcurrentHashMap<>();
                for (String targetKey : targets.keySet()) {
                    UUID target = uuid(targetKey);
                    if (target == null || !targets.get(targetKey).isJsonObject()) continue;
                    JsonObject f = targets.getAsJsonObject(targetKey);
                    Flags flags = new Flags(bool(f, "autoMoney"), bool(f, "ignore"), bool(f, "autoTpa"),
                            bool(f, "autoTpaHere"));
                    if (!flags.isDefault()) row.put(target, flags);
                }
                if (!row.isEmpty()) SETTINGS.put(viewer, row);
            }
            loaded = true;
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        SETTINGS.forEach((viewer, row) -> {
            JsonObject targets = new JsonObject();
            row.forEach((target, flags) -> {
                JsonObject f = new JsonObject();
                if (flags.autoMoney()) f.addProperty("autoMoney", true);
                if (flags.ignore()) f.addProperty("ignore", true);
                if (flags.autoTpa()) f.addProperty("autoTpa", true);
                if (flags.autoTpaHere()) f.addProperty("autoTpaHere", true);
                targets.add(target.toString(), f);
            });
            root.add(viewer.toString(), targets);
        });
        MinestarFiles.write(path(), root);
    }

    private static boolean bool(JsonObject json, String key) {
        return json.has(key) && json.get(key).getAsBoolean();
    }

    private static UUID uuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null; // a hand-written key that is not a uuid: skip the row, keep the rest
        }
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
