package com.roll_54.roll_mod.minestar.level;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import com.roll_54.roll_mod.minestar.friends.FriendsService;
import com.roll_54.roll_mod.network.packet.LevelRewardsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Follows each player's Minestar level and applies what it gives ({@link LevelPerks}), ported from Eden_LD: bonus
 * chunk claims go on the player's own Open Parties and Claims limit (as a tracked difference, so a level dropping or
 * the rules changing takes back only what this gave), free daily rerolls are read where they are counted, and emoji
 * and sticker slots by whatever counts those. The level is fetched on login and every half hour for players online,
 * and kept in {@code minestar/levels.json} so perks hold before the next fetch. Players are told what a new level
 * gave them.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ProfileLevels {

    private static final String FILE = "levels.json";
    private static final int REFRESH_TICKS = 20 * 60 * 30;

    /** Per player: the level last seen and the bonus claims this has put on their OPAC limit. */
    private record Entry(int level, int claims) {}

    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private ProfileLevels() {}

    /** The player's Minestar level as last seen; 0 before the first fetch or without Minestar. */
    public static int level(UUID player) {
        load();
        Entry entry = ENTRIES.get(player);
        return entry == null ? 0 : entry.level();
    }

    /** The player's current total of a perk. */
    public static int perk(UUID player, LevelPerks.Kind kind) {
        return LevelPerks.total(kind, level(player));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % REFRESH_TICKS != 0) return;
        server.getPlayerList().getPlayers().forEach(ProfileLevels::refresh);
    }

    /** Asks Minestar for the player's level (cached by FriendsService), then applies it on the server thread. */
    public static void refresh(ServerPlayer player) {
        UUID id = player.getUUID();
        MinecraftServer server = player.server;
        // What is known already, at once: the rules and the stored level.
        send(player, level(id));
        try {
            FriendsService.detail(id).whenComplete((detail, error) -> {
                if (error != null || detail == null) return;
                server.execute(() -> update(server, id, Math.max(0, detail.level())));
            });
        } catch (RuntimeException e) {
            RollMod.LOGGER.debug("[Levels] no Minestar profile for {}", id, e);
        }
    }

    private static void update(MinecraftServer server, UUID id, int now) {
        load();
        Entry before = ENTRIES.get(id);
        boolean seen = before != null;
        int claims = applyClaims(server, id, now, seen ? before.claims() : 0);
        Entry next = new Entry(now, claims);
        if (!next.equals(before)) {
            ENTRIES.put(id, next);
            save();
        }
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) return;
        send(player, now);
        if (!seen) introduce(player, now);
        else if (now > before.level()) announce(player, before.level(), now);
    }

    private static void send(ServerPlayer player, int level) {
        List<String> rules = LevelPerks.rules().stream().map(LevelPerks.Rule::write).toList();
        PacketDistributor.sendToPlayer(player, new LevelRewardsPacket(level, rules));
    }

    /** Puts the level's bonus claims on the player's OPAC limit: only the difference to what this put there. */
    private static int applyClaims(MinecraftServer server, UUID id, int level, int applied) {
        if (!ModList.get().isLoaded("openpartiesandclaims")) return applied;
        int target = LevelPerks.total(LevelPerks.Kind.CLAIMS, level);
        if (target == applied) return applied;
        try {
            return Opac.addBonus(server, id, target - applied) ? target : applied;
        } catch (RuntimeException | LinkageError e) {
            RollMod.LOGGER.warn("[Levels] could not change the OPAC bonus claims of {}", id, e);
            return applied;
        }
    }

    /** Kept apart so OPAC's classes are only resolved with it loaded. */
    private static final class Opac {
        static boolean addBonus(MinecraftServer server, UUID player, int delta) {
            var config = xaero.pac.common.server.api.OpenPACServerAPI.get(server).getPlayerConfigManager()
                    .getLoadedConfig(player);
            Integer raw = config.getRaw(xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions.BONUS_CHUNK_CLAIMS);
            int next = Math.max(0, (raw == null ? 0 : raw) + delta);
            var result = config.tryToSet(xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions.BONUS_CHUNK_CLAIMS, next);
            if (result != xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI.SetResult.SUCCESS) {
                RollMod.LOGGER.warn("[Levels] OPAC refused bonus claims {} for {}: {}", next, player, result);
                return false;
            }
            return true;
        }
    }

    /** The first time the level is seen: what it already gives. */
    private static void introduce(ServerPlayer player, int level) {
        Map<LevelPerks.Kind, Integer> totals = LevelPerks.totals(LevelPerks.rules(), level);
        if (level <= 0 || totals.isEmpty()) return;
        player.sendSystemMessage(Component.translatable("msg.roll_mod.level.perks", level).withStyle(ChatFormatting.GOLD)
                .append(" ").append(LevelPerks.describe(totals).copy().withStyle(ChatFormatting.GREEN)));
    }

    /** Level-up: what the new level(s) gave, or what the next perk is. */
    private static void announce(ServerPlayer player, int before, int now) {
        List<LevelPerks.Rule> rules = LevelPerks.rules();
        Map<LevelPerks.Kind, Integer> gained = new EnumMap<>(LevelPerks.Kind.class);
        for (int level = before + 1; level <= now; level++) {
            for (LevelPerks.Gain gain : LevelPerks.at(rules, level)) gained.merge(gain.kind(), gain.amount(), Integer::sum);
        }
        MutableComponent message = Component.translatable("msg.roll_mod.level.up", now).withStyle(ChatFormatting.GOLD);
        if (!gained.isEmpty()) {
            message.append(" ").append(Component.translatable("msg.roll_mod.level.gained", LevelPerks.describe(gained))
                    .withStyle(ChatFormatting.GREEN));
        } else {
            int next = LevelPerks.nextPerkLevel(rules, now);
            if (next > 0) {
                message.append(" ").append(Component.translatable("msg.roll_mod.level.next", next,
                        LevelPerks.describeAt(rules, next)).withStyle(ChatFormatting.GRAY));
            }
        }
        player.sendSystemMessage(message);
    }

    /* ------------------------------------------ persistence ----------------------------------------- */

    private static void load() {
        if (loaded) return;
        synchronized (ProfileLevels.class) {
            if (loaded) return;
            JsonObject root = MinestarFiles.readObject(MinestarFiles.resolve(FILE));
            for (String key : root.keySet()) {
                try {
                    JsonObject row = root.getAsJsonObject(key);
                    ENTRIES.put(UUID.fromString(key), new Entry(row.has("level") ? row.get("level").getAsInt() : 0,
                            row.has("claims") ? row.get("claims").getAsInt() : 0));
                } catch (RuntimeException ignored) {
                    // A hand-edited row that does not read: skip it, keep the rest.
                }
            }
            loaded = true;
        }
    }

    private static synchronized void save() {
        JsonObject root = new JsonObject();
        ENTRIES.forEach((id, entry) -> {
            JsonObject row = new JsonObject();
            row.addProperty("level", entry.level());
            row.addProperty("claims", entry.claims());
            root.add(id.toString(), row);
        });
        MinestarFiles.write(MinestarFiles.resolve(FILE), root);
    }
}
