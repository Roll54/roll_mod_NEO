package com.roll_54.roll_mod.minestar.op;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * {@code minestar/operators.json} — who is an operator, as a file rather than as whoever last ran
 * {@code /op}.
 *
 * <p>The file is the authority: it is applied when the server starts and again whenever a listed
 * player joins, so an {@code /op} typed by hand is reverted on that player's next login. That is
 * the point of it — one place to read, and one place to change.
 *
 * <p>{@code administrators} is the anchor. Only those names may flip the switches, and none of them
 * can be switched off from inside the game: a permission that can revoke itself is one bad click
 * away from locking everybody out, with no way back in without editing the file anyway.
 */
public final class OperatorStore {

    private static final String FILE = "operators.json";

    private static final List<OperatorEntry> ENTRIES = new ArrayList<>();
    private static final Set<String> ADMINISTRATORS = new LinkedHashSet<>();
    private static volatile boolean loaded;

    private OperatorStore() {}

    /* -------------------------------------------- reading ------------------------------------------- */

    public static synchronized List<OperatorEntry> entries() {
        load();
        return List.copyOf(ENTRIES);
    }

    /** Whether this player may see and flip the switches. */
    public static synchronized boolean canManage(ServerPlayer player) {
        load();
        return ADMINISTRATORS.contains(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
    }

    /** Whether this name is an anchor, and so may not be switched off from the game. */
    public static synchronized boolean isAdministrator(String name) {
        load();
        return ADMINISTRATORS.contains(name.toLowerCase(Locale.ROOT));
    }

    /* -------------------------------------------- writing ------------------------------------------- */

    /**
     * Sets a row and applies it at once.
     *
     * @return whether anything changed.
     */
    public static synchronized boolean set(MinecraftServer server, UUID id, String name, boolean op) {
        load();

        if (!op && isAdministrator(name)) return false;

        OperatorEntry existing = byId(id, name);
        OperatorEntry updated = existing == null
                ? new OperatorEntry(name, id, op, OperatorEntry.DEFAULT_LEVEL)
                : existing.withId(id).withOp(op);

        if (existing != null) ENTRIES.remove(existing);
        ENTRIES.add(updated);
        save();
        apply(server, updated);
        return true;
    }

    /* ------------------------------------------- applying ------------------------------------------- */

    /** Brings the whole server in line with the file. Called once the server is up. */
    public static synchronized void applyAll(MinecraftServer server) {
        load();
        for (OperatorEntry entry : List.copyOf(ENTRIES)) {
            apply(server, entry);
        }
    }

    /**
     * Brings one player in line with the file, and fills in their id the first time it sees them.
     *
     * <p>Matching by name until then is what lets an operator add a row with nothing but a
     * nickname, which is the only thing they reliably know.
     */
    public static synchronized void applyTo(ServerPlayer player) {
        load();
        OperatorEntry entry = byId(player.getUUID(), player.getGameProfile().getName());
        if (entry == null) return;

        if (entry.id() == null) {
            ENTRIES.remove(entry);
            entry = entry.withId(player.getUUID());
            ENTRIES.add(entry);
            save();
        }
        apply(player.server, entry);
    }

    private static void apply(MinecraftServer server, OperatorEntry entry) {
        GameProfile profile = profile(server, entry);
        if (profile == null) return; // never seen this name; nothing to op yet

        PlayerList players = server.getPlayerList();
        boolean isOp = players.isOp(profile);
        if (entry.op() == isOp) return;

        if (entry.op()) {
            players.op(profile);
        } else {
            players.deop(profile);
        }
        RollMod.LOGGER.info("[Operators] {} is now {}", profile.getName(),
                entry.op() ? "an operator" : "not an operator");
    }

    private static @Nullable GameProfile profile(MinecraftServer server, OperatorEntry entry) {
        if (entry.id() != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(entry.id());
            if (online != null) return online.getGameProfile();
        }
        ServerPlayer byName = server.getPlayerList().getPlayerByName(entry.name());
        if (byName != null) return byName.getGameProfile();
        return server.getProfileCache() == null
                ? null
                : server.getProfileCache().get(entry.name()).orElse(null);
    }

    /* ------------------------------------------ persistence ----------------------------------------- */

    public static synchronized void reload() {
        ENTRIES.clear();
        ADMINISTRATORS.clear();
        loaded = false;
    }

    private static @Nullable OperatorEntry byId(@Nullable UUID id, String name) {
        for (OperatorEntry entry : ENTRIES) {
            if (id != null && id.equals(entry.id())) return entry;
            if (entry.id() == null && entry.name().equalsIgnoreCase(name)) return entry;
        }
        return null;
    }

    private static void load() {
        if (loaded) return;

        JsonObject root = MinestarFiles.readObject(path());
        if (root.has("administrators")) {
            for (JsonElement element : root.getAsJsonArray("administrators")) {
                ADMINISTRATORS.add(element.getAsString().toLowerCase(Locale.ROOT));
            }
        }
        if (root.has("players")) {
            for (JsonElement element : root.getAsJsonArray("players")) {
                JsonObject row = element.getAsJsonObject();
                if (!row.has("name")) continue;
                UUID id = null;
                if (row.has("uuid") && !row.get("uuid").getAsString().isBlank()) {
                    try {
                        id = UUID.fromString(row.get("uuid").getAsString());
                    } catch (IllegalArgumentException e) {
                        // A typo in a hand-written id: fall back to matching by name.
                    }
                }
                ENTRIES.add(new OperatorEntry(row.get("name").getAsString(), id,
                        row.has("op") && row.get("op").getAsBoolean(),
                        row.has("level") ? row.get("level").getAsInt() : OperatorEntry.DEFAULT_LEVEL));
            }
        }
        loaded = true;

        if (root.size() == 0) save(); // first run: leave an empty file to edit rather than nothing
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", "op: true grants operator rights on join and at once when "
                + "toggled in the hub. Only names in 'administrators' may flip the switches, and "
                + "they cannot be switched off from in game. Run /rollmod op reload after editing.");

        JsonArray administrators = new JsonArray();
        ADMINISTRATORS.forEach(administrators::add);
        root.add("administrators", administrators);

        JsonArray players = new JsonArray();
        for (OperatorEntry entry : ENTRIES) {
            JsonObject row = new JsonObject();
            row.addProperty("name", entry.name());
            row.addProperty("uuid", entry.id() == null ? "" : entry.id().toString());
            row.addProperty("op", entry.op());
            row.addProperty("level", entry.level());
            players.add(row);
        }
        root.add("players", players);

        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
