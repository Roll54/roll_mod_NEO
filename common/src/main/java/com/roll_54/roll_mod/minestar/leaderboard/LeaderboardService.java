package com.roll_54.roll_mod.minestar.leaderboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.model.BalanceEntry;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyLedger;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.network.packet.LeaderboardPackets;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Builds the leaderboard tab's boards on the server and answers each request with the top rows and
 * the viewer's own line.
 *
 * <p>Every board is this season's: balances and the money history (both emptied by the admin wipe),
 * this world's quest file, this world's statistics files and daily-task counts. Boards are cached for
 * {@link #TTL_MILLIS}, because two of them are not cheap — the playtime board reads every player's
 * statistics file, and the money boards are database queries — and a board a couple of minutes old
 * is still the board.
 *
 * <p>Threading: database and file work run off the server thread; anything touching the game —
 * online players, the quest file, the profile cache for names — runs on it.
 */
public final class LeaderboardService {

    /** Rows sent per board. */
    public static final int TOP = 50;
    private static final long TTL_MILLIS = 120_000L;
    private static final long WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000;

    /**
     * A built board. {@code all} holds every ranked entry when the board was computed in full
     * (playtime, quests, daily tasks), so the viewer's exact rank can be read from it; the money boards only fetch
     * the top and look the viewer up separately.
     */
    private record Board(List<LeaderboardRow> top, @Nullable List<LeaderboardRow> all, long at) {}

    private static final Map<LeaderboardCategory, Board> CACHE = new EnumMap<>(LeaderboardCategory.class);
    private static final Map<LeaderboardCategory, CompletableFuture<Board>> BUILDING =
            new EnumMap<>(LeaderboardCategory.class);

    private LeaderboardService() {}

    /** Sends {@code viewer} the board, building it first when there is no fresh one. Server thread. */
    public static void request(ServerPlayer viewer, LeaderboardCategory category) {
        MinecraftServer server = viewer.server;
        UUID id = viewer.getUUID();
        board(server, category).whenComplete((board, error) -> server.execute(() -> {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online == null) return;
            if (error != null || board == null) {
                RollMod.LOGGER.warn("[Leaderboards] could not build {}", category, error);
                PacketDistributor.sendToPlayer(online, new LeaderboardPackets.Sync(category, List.of(), null, true));
                return;
            }
            own(online, category, board).thenAccept(own -> server.execute(() -> {
                ServerPlayer still = server.getPlayerList().getPlayer(id);
                if (still != null) {
                    PacketDistributor.sendToPlayer(still, new LeaderboardPackets.Sync(category, board.top(), own, false));
                }
            }));
        }));
    }

    private static CompletableFuture<Board> board(MinecraftServer server, LeaderboardCategory category) {
        Board cached = CACHE.get(category);
        if (cached != null && System.currentTimeMillis() - cached.at() < TTL_MILLIS) {
            return CompletableFuture.completedFuture(cached);
        }
        CompletableFuture<Board> pending = BUILDING.get(category);
        if (pending != null) return pending;

        CompletableFuture<Board> built = switch (category) {
            case RICHEST -> CurrencyRepository.getTop(CurrencyType.MAIN, TOP)
                    .thenCompose(entries -> named(server, entries));
            case EARNERS_7D -> CurrencyLedger.topEarners(CurrencyType.MAIN, System.currentTimeMillis() - WEEK_MILLIS, TOP)
                    .thenCompose(entries -> named(server, entries));
            case PLAYTIME -> CompletableFuture.supplyAsync(() -> readPlaytimes(server), Util.backgroundExecutor())
                    .thenCompose(fromFiles -> onServer(server, () -> playtimeBoard(server, fromFiles)));
            case QUESTS -> onServer(server, LeaderboardService::questBoard);
            case DAILY_TASKS -> onServer(server, () -> dailyTasksBoard(server));
        };
        BUILDING.put(category, built);
        return built.whenComplete((board, error) -> server.execute(() -> {
            BUILDING.remove(category);
            if (board != null) CACHE.put(category, board);
        }));
    }

    /* ---------------------------------------------- money ---------------------------------------------- */

    /** Names the database's uuids on the server thread (the profile cache), keeping the order. */
    private static CompletableFuture<Board> named(MinecraftServer server, List<BalanceEntry> entries) {
        return onServer(server, () -> {
            List<LeaderboardRow> rows = new ArrayList<>(entries.size());
            for (int i = 0; i < entries.size(); i++) {
                BalanceEntry entry = entries.get(i);
                rows.add(new LeaderboardRow(i + 1, entry.uuid(), name(server, entry.uuid()), entry.balance()));
            }
            return new Board(List.copyOf(rows), null, System.currentTimeMillis());
        });
    }

    /* --------------------------------------------- playtime -------------------------------------------- */

    /**
     * Every player's {@code minecraft:play_time} (ticks) from this world's statistics files. Off the
     * server thread: one file per player who ever joined. Online players' files lag behind their live
     * counter, which {@link #playtimeBoard} applies on top.
     */
    private static Map<UUID, Long> readPlaytimes(MinecraftServer server) {
        Map<UUID, Long> out = new HashMap<>();
        Path dir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        if (!Files.isDirectory(dir)) return out;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                UUID id;
                try {
                    id = UUID.fromString(fileName.substring(0, fileName.length() - ".json".length()));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonElement root = JsonParser.parseReader(reader);
                    long ticks = playTime(root);
                    if (ticks > 0) out.put(id, ticks);
                } catch (Exception e) {
                    RollMod.LOGGER.debug("[Leaderboards] unreadable stats file {}", file, e);
                }
            }
        } catch (IOException e) {
            RollMod.LOGGER.warn("[Leaderboards] could not list {}", dir, e);
        }
        return out;
    }

    private static long playTime(JsonElement root) {
        if (!root.isJsonObject()) return 0;
        JsonObject stats = root.getAsJsonObject().getAsJsonObject("stats");
        if (stats == null) return 0;
        JsonObject custom = stats.getAsJsonObject("minecraft:custom");
        if (custom == null || !custom.has("minecraft:play_time")) return 0;
        return custom.get("minecraft:play_time").getAsLong();
    }

    private static Board playtimeBoard(MinecraftServer server, Map<UUID, Long> fromFiles) {
        Map<UUID, Long> ticks = new HashMap<>(fromFiles);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ticks.put(player.getUUID(), (long) player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)));
        }
        return ranked(server, ticks);
    }

    /* ------------------------------------------- daily tasks ------------------------------------------- */

    /** Task rewards each player has claimed, from the daily tasks' saved data. Server thread. */
    private static Board dailyTasksBoard(MinecraftServer server) {
        Map<UUID, Long> done = new HashMap<>();
        DailyTaskManager.state(server).tasksDone.forEach((id, count) -> {
            if (count > 0) done.put(id, (long) count);
        });
        return ranked(server, done);
    }

    /* ---------------------------------------------- quests --------------------------------------------- */

    private static Board questBoard() {
        List<LeaderboardRow> all = ModList.get().isLoaded("ftbquests") ? Quests.rows() : List.of();
        return new Board(List.copyOf(all.subList(0, Math.min(TOP, all.size()))), all, System.currentTimeMillis());
    }

    /** Isolated holder for the FTB Quests API, only touched when the mod is loaded. */
    private static final class Quests {

        /** Required quests completed per team, the rule {@code QuestProgress} uses, best first. */
        static List<LeaderboardRow> rows() {
            dev.ftb.mods.ftbquests.quest.BaseQuestFile file =
                    dev.ftb.mods.ftbquests.api.FTBQuestsAPI.api().getQuestFile(false);
            if (file == null) return List.of();
            List<dev.ftb.mods.ftbquests.quest.Quest> quests =
                    file.collect(dev.ftb.mods.ftbquests.quest.Quest.class);
            List<LeaderboardRow> rows = new ArrayList<>();
            for (dev.ftb.mods.ftbquests.quest.TeamData data : file.getAllTeamData()) {
                long done = 0;
                for (dev.ftb.mods.ftbquests.quest.Quest quest : quests) {
                    if (!quest.isOptionalForProgression(data) && data.isCompleted(quest)) done++;
                }
                if (done > 0) rows.add(new LeaderboardRow(0, data.getTeamId(), data.getName(), done));
            }
            rows.sort(Comparator.comparingLong(LeaderboardRow::value).reversed());
            List<LeaderboardRow> ranked = new ArrayList<>(rows.size());
            for (int i = 0; i < rows.size(); i++) {
                LeaderboardRow row = rows.get(i);
                ranked.add(new LeaderboardRow(i + 1, row.id(), row.name(), row.value()));
            }
            return ranked;
        }

        @Nullable
        static UUID teamOf(ServerPlayer player) {
            dev.ftb.mods.ftbquests.quest.BaseQuestFile file =
                    dev.ftb.mods.ftbquests.api.FTBQuestsAPI.api().getQuestFile(false);
            if (file == null) return null;
            return file.getTeamData(player).map(dev.ftb.mods.ftbquests.quest.TeamData::getTeamId).orElse(null);
        }
    }

    /* --------------------------------------------- own line -------------------------------------------- */

    /**
     * The viewer's own line: their rank and value, or rank 0 when they are not on the board at all.
     * From the board itself when it was built in full; from the database otherwise.
     */
    private static CompletableFuture<LeaderboardRow> own(ServerPlayer viewer, LeaderboardCategory category, Board board) {
        UUID id = viewer.getUUID();
        String name = viewer.getGameProfile().getName();
        UUID key = id;
        if (category == LeaderboardCategory.QUESTS) {
            key = ModList.get().isLoaded("ftbquests") ? Quests.teamOf(viewer) : null;
            if (key == null) return CompletableFuture.completedFuture(new LeaderboardRow(0, id, name, 0));
        }
        List<LeaderboardRow> search = board.all() != null ? board.all() : board.top();
        for (LeaderboardRow row : search) {
            if (row.id().equals(key)) return CompletableFuture.completedFuture(row);
        }
        return switch (category) {
            case RICHEST -> CurrencyRepository.getBalance(id, CurrencyType.MAIN)
                    .thenCompose(balance -> balance <= 0
                            ? CompletableFuture.completedFuture(new LeaderboardRow(0, id, name, 0))
                            : CurrencyRepository.rankOf(CurrencyType.MAIN, balance)
                                    .thenApply(rank -> new LeaderboardRow(rank.intValue(), id, name, balance)));
            // Past the top rows of the earners board the exact rank is not worth a query of its own.
            case EARNERS_7D -> CurrencyLedger.earnings(id, CurrencyType.MAIN, System.currentTimeMillis() - WEEK_MILLIS)
                    .thenApply(sum -> new LeaderboardRow(0, id, name, sum));
            default -> CompletableFuture.completedFuture(new LeaderboardRow(0, id, name, 0));
        };
    }

    /* --------------------------------------------- helpers --------------------------------------------- */

    /** A full board from per-player values: best first, ranked from 1, named. Server thread. */
    private static Board ranked(MinecraftServer server, Map<UUID, Long> values) {
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(values.entrySet());
        sorted.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());
        List<LeaderboardRow> all = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<UUID, Long> entry = sorted.get(i);
            all.add(new LeaderboardRow(i + 1, entry.getKey(), name(server, entry.getKey()), entry.getValue()));
        }
        return new Board(List.copyOf(all.subList(0, Math.min(TOP, all.size()))), all, System.currentTimeMillis());
    }

    private static String name(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) return online.getGameProfile().getName();
        if (server.getProfileCache() != null) {
            String cached = server.getProfileCache().get(id).map(GameProfile::getName).orElse(null);
            if (cached != null) return cached;
        }
        return id.toString().substring(0, 8);
    }

    private static <T> CompletableFuture<T> onServer(MinecraftServer server, java.util.function.Supplier<T> work) {
        return CompletableFuture.supplyAsync(work, server);
    }

    /**
     * The name a cached player board shows for {@code id}, or null when no board lists them. What
     * lets a leaderboard row be opened as a profile: being on a public board is what makes the
     * player's profile something the viewer may look at. Team boards list teams, not players.
     */
    @Nullable
    public static String rankedName(UUID id) {
        for (Map.Entry<LeaderboardCategory, Board> entry : CACHE.entrySet()) {
            if (entry.getKey().teams()) continue;
            for (LeaderboardRow row : entry.getValue().top()) {
                if (row.id().equals(id)) return row.name();
            }
        }
        return null;
    }

    /** Season reset: forget the cached boards so the next view is built from the emptied data. */
    public static void invalidate() {
        CACHE.clear();
    }
}
