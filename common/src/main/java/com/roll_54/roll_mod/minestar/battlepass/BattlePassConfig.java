package com.roll_54.roll_mod.minestar.battlepass;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The battle pass's settings, in {@code config/minestar/battlepass.json} (written with defaults the first time):
 * the season, how many points a level costs and what each source pays, the quest that opens each block of levels,
 * and the 128 free and 64 Prime rewards.
 *
 * <p>Rewards are written as {@code "namespace:item*count"} or {@code "money:amount"}.
 */
public final class BattlePassConfig {

    public static final int FREE_LEVELS = 128;
    public static final int PRIME_LEVELS = 64;
    /** Free levels per quest block: 8 blocks of 16. Prime has 8 per block. */
    public static final int FREE_PER_BLOCK = 16;
    public static final int PRIME_PER_BLOCK = 8;
    public static final int BLOCKS = 8;

    /** Bumped when an update rewrites older rewards once (redstone → roll_mod's, no ingots past MV). */
    public int configVersion = 0;
    public int season = 1;
    /** The (06:00-shifted) day the season began; the twice-weekly task batches count from it. */
    public long seasonStartDay = Long.MIN_VALUE;
    public int pointsPerLevel = 100;
    public int pointsPerPrimeLevel = 200;
    // Sized so a month of active play finishes the pass (12 800 points either track). Over 30 days a free player
    // gets 4 dailies x 50 x 30 = 6 000, and ~8.6 Monday/Thursday batches x (3 x 100 + 400) = ~6 000, so ~12 000 from
    // dailies and weeklies alone; the seasonal tasks (5 x 1 000) close the rest, done around day 25. Prime adds its
    // daily slot (+1 500) and weekly task (+~860): ~14 400 without the seasonal ones.
    public int pointsDaily = 50;
    public int pointsEasy = 100;
    public int pointsHard = 400;
    public int pointsPrimeTask = 100;
    /** A battle-pass task asks for the daily task's base amount times this. */
    public int easyMultiplier = 5;
    public int hardMultiplier = 12;
    /**
     * Per block, the FTB quest (hex id) that must be done to claim that block's rewards; empty for none. Points keep
     * coming either way — only claiming waits. Defaults: the first quest of each main chapter (bronze, steel, LV …).
     */
    public List<String> gates = new ArrayList<>(List.of("", "7E429723D4077654", "30C00BE8D6E26A61",
            "22C9182A74642D38", "4B1CFC64CB262C44", "1A0F200E4BD286C3", "04F0701F424CEAD7", "345992D36C79E220"));
    /**
     * The weekly pool the Monday/Thursday batches draw from: these daily tasks at a bigger amount (not all of them —
     * jumping and raids are left out) …
     */
    public List<String> weeklyDailies = new ArrayList<>(List.of("clip_agri_crops", "breed_animals",
            "kill_arthropods", "kill_zombies", "kill_skeletons", "kill_witches", "kill_undead", "random_fruit_dish",
            "walk_distance", "tide_rare"));
    /** … the pass's own weekly challenges ({@code BattlePassChallenges}), at their own amount … */
    public List<String> weeklyChallenges = new ArrayList<>(List.of("bp_kill_creepers", "bp_kill_endermen",
            "bp_kill_wither_skeletons", "bp_kill_hoglins", "bp_kill_breezes", "bp_kill_bogged",
            "bp_kill_illagers", "bp_kill_drowned", "bp_catch_treasure", "bp_walk_marathon",
            "bp_cook_dishes", "bp_win_raid", "bp_catch_tide_fish"));
    /** The season's tasks: long, hard challenges only, {@link #seasonalCount} of them dealt each season. */
    public List<String> seasonalChallenges = new ArrayList<>(List.of(LEGENDS[0], LEGENDS[1], LEGENDS[2], LEGENDS[3],
            LEGENDS[4], LEGENDS[5], LEGENDS[6], "bp_season_tide_fish",
            "bp_season_wither", "bp_season_warden",
            "bp_season_angler", "bp_season_monsters", "bp_season_explorer", "bp_season_rancher", "bp_season_chef",
            "bp_season_raids", "bp_season_epic_fish", "bp_season_breezes", "bp_season_mace"));
    public int seasonalCount = 5;
    /** The seasonals for Tide's seven legendary fish, one each. */
    private static final String[] LEGENDS = {"bp_season_legend_coelacanth", "bp_season_legend_shooting_starfish", "bp_season_legend_devils_hole_pupfish", "bp_season_legend_midas_fish", "bp_season_legend_alpha_fish", "bp_season_legend_dragon_fish", "bp_season_legend_voidseeker"};
    public int pointsSeasonal = 1000;
    public List<String> free = new ArrayList<>(BattlePassRewards.defaultFree());
    public List<String> prime = new ArrayList<>(BattlePassRewards.defaultPrime());

    private static BattlePassConfig current;

    private static Path file() {
        return MinestarFiles.resolve("battlepass.json");
    }

    public static synchronized BattlePassConfig get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void reload() {
        current = load();
    }

    /** The file's modification time as last read or written here; another one means it was edited by hand. */
    private static transient long stamp;

    private static long modified() {
        try {
            return java.nio.file.Files.getLastModifiedTime(file()).toMillis();
        } catch (java.io.IOException e) {
            return 0;
        }
    }

    /** Reloads the file if it was changed outside the game since; true when it did. */
    public static synchronized boolean reloadIfEdited() {
        if (current == null || modified() == stamp) return false;
        current = load();
        return true;
    }

    public synchronized void save() {
        MinestarFiles.write(file(), MinestarFiles.GSON.toJsonTree(this));
        stamp = modified();
    }

    private static BattlePassConfig load() {
        JsonObject json = MinestarFiles.readObject(file());
        BattlePassConfig config = json.size() == 0 ? new BattlePassConfig()
                : MinestarFiles.GSON.fromJson(json, BattlePassConfig.class);
        if (config == null) config = new BattlePassConfig();
        BattlePassConfig defaults = new BattlePassConfig();
        if (config.weeklyDailies == null) config.weeklyDailies = defaults.weeklyDailies;
        if (config.weeklyChallenges == null) config.weeklyChallenges = defaults.weeklyChallenges;
        if (config.seasonalChallenges == null) config.seasonalChallenges = defaults.seasonalChallenges;
        if (config.seasonalCount <= 0) config.seasonalCount = 5;
        if (config.gates == null) config.gates = new ArrayList<>();
        while (config.gates.size() < BLOCKS) config.gates.add("");
        if (config.free == null || config.free.size() < FREE_LEVELS) config.free = new ArrayList<>(BattlePassRewards.defaultFree());
        if (config.prime == null || config.prime.size() < PRIME_LEVELS) config.prime = new ArrayList<>(BattlePassRewards.defaultPrime());
        // Once, for a config from before version 2; after that an admin may put whatever they like back.
        if (config.configVersion < 2) {
            config.free.replaceAll(BattlePassRewards::migrate);
            config.prime.replaceAll(BattlePassRewards::migrate);
            config.configVersion = 2;
        }
        // Version 3: challenges added since — appended once to an older file's list.
        if (config.configVersion < 3) {
            for (String id : List.of("bp_catch_epic_fish")) if (!config.weeklyChallenges.contains(id)) config.weeklyChallenges.add(id);
            for (String id : List.of("bp_season_shulkers", "bp_season_brutes", "bp_season_golden_apples")) {
                if (!config.seasonalChallenges.contains(id)) config.seasonalChallenges.add(id);
            }
            config.configVersion = 3;
        }
        // Version 4: no seasonal tasks about gathering vanilla resources (diamonds, ancient debris, golden apples);
        // the epic Tide fish instead. The challenges themselves stay registered, just out of the pool.
        if (config.configVersion < 4) {
            config.seasonalChallenges.removeAll(List.of("bp_season_ancient_debris", "bp_season_diamonds",
                    "bp_season_golden_apples"));
            if (!config.seasonalChallenges.contains("bp_season_epic_fish")) config.seasonalChallenges.add("bp_season_epic_fish");
            config.configVersion = 4;
        }
        // Version 5: the shulker, dragon and elder guardian seasonals taken out (still registered, out of the pool).
        if (config.configVersion < 5) {
            config.seasonalChallenges.removeAll(List.of("bp_season_shulkers", "bp_season_dragon", "bp_season_elder_guardian"));
            config.configVersion = 5;
        }
        // Version 6: the phantom weekly taken out of the pool (still registered).
        if (config.configVersion < 6) {
            config.weeklyChallenges.remove("bp_kill_phantoms");
            config.configVersion = 6;
        }
        // Version 7: one seasonal per legendary fish instead of "any five legendaries".
        if (config.configVersion < 7) {
            int at = config.seasonalChallenges.indexOf("bp_season_legendary_fish");
            config.seasonalChallenges.remove("bp_season_legendary_fish");
            for (String id : LEGENDS) {
                if (config.seasonalChallenges.contains(id)) continue;
                if (at >= 0) config.seasonalChallenges.add(at++, id);
                else config.seasonalChallenges.add(id);
            }
            config.configVersion = 7;
        }
        // Version 8: a weekly per epic Tide fish instead of "any two epic fish".
        if (config.configVersion < 8) {
            config.weeklyChallenges.remove("bp_catch_epic_fish");
            for (String id : com.roll_54.roll_mod.minestar.dailytasks.TideFishTasks.epicIds()) {
                if (!config.weeklyChallenges.contains(id)) config.weeklyChallenges.add(id);
            }
            config.configVersion = 8;
        }
        // Version 9: the golden carrot weekly taken out (too easy; still registered).
        if (config.configVersion < 9) {
            config.weeklyChallenges.remove("bp_craft_golden_carrots");
            config.configVersion = 9;
        }
        // Version 10: a harder week. The trivial tasks out of the pool (they stay registered), and the scaled-up
        // dailies asked for at 5x / 12x instead of 3x / 8x — unless an admin had already set their own multipliers.
        if (config.configVersion < 10) {
            config.weeklyDailies.removeAll(List.of("mine_stone", "craft_items", "eat_food", "playtime",
                    "shear_mooshroom", "shear_sheep", "go_fishing"));
            config.weeklyChallenges.removeAll(List.of("bp_craft_cakes", "bp_drink_honey", "bp_smelt_glass",
                    "bp_smelt_bricks", "bp_smelt_iron", "bp_craft_rails", "bp_mine_obsidian", "bp_eat_golden_apples",
                    "bp_breed_bees", "bp_breed_horses", "bp_catch_fish"));
            if (config.easyMultiplier == 3) config.easyMultiplier = 5;
            if (config.hardMultiplier == 8) config.hardMultiplier = 12;
            config.configVersion = 10;
        }
        // Version 11: a claimed daily pays 25 points, not 20 (unless an admin had set their own).
        if (config.configVersion < 11) {
            if (config.pointsDaily == 20) config.pointsDaily = 25;
            config.configVersion = 11;
        }
        // Version 12: no weekly tasks about gathering or processing vanilla resources (ores, diamonds, logs, smelting).
        if (config.configVersion < 12) {
            config.weeklyDailies.removeAll(List.of("chop_logs", "mine_ores", "smelt_cooked_meat", "smelt_items"));
            config.weeklyChallenges.removeAll(List.of("bp_mine_diamonds", "bp_mine_deep_ores", "bp_mine_ancient_debris"));
            config.configVersion = 12;
        }
        // Version 13: an easy (and a Prime) weekly task pays 50 points, not 60 (unless an admin had set their own).
        if (config.configVersion < 13) {
            if (config.pointsEasy == 60) config.pointsEasy = 50;
            if (config.pointsPrimeTask == 60) config.pointsPrimeTask = 50;
            config.configVersion = 13;
        }
        // Version 14: blazes, slimes, guardians and piglin brutes out; the Trial Chambers in (breezes, bogged, the mace).
        if (config.configVersion < 14) {
            config.weeklyChallenges.removeAll(List.of("bp_kill_blazes", "bp_kill_slimes", "bp_kill_guardians"));
            for (String id : List.of("bp_kill_breezes", "bp_kill_bogged")) if (!config.weeklyChallenges.contains(id)) config.weeklyChallenges.add(id);
            config.seasonalChallenges.remove("bp_season_brutes");
            for (String id : List.of("bp_season_breezes", "bp_season_mace")) if (!config.seasonalChallenges.contains(id)) config.seasonalChallenges.add(id);
            config.configVersion = 14;
        }
        // Version 15: rare Tide fish as a weekly task (the daily "rare catch", at the weekly amount).
        if (config.configVersion < 15) {
            if (!config.weeklyDailies.contains("tide_rare")) config.weeklyDailies.add("tide_rare");
            config.configVersion = 15;
        }
        // Version 16: points doubled so a month of active play finishes the pass (see pointsDaily), unless an admin
        // had set their own.
        if (config.configVersion < 16) {
            if (config.pointsDaily == 25) config.pointsDaily = 50;
            if (config.pointsEasy == 50) config.pointsEasy = 100;
            if (config.pointsHard == 200) config.pointsHard = 400;
            if (config.pointsPrimeTask == 50) config.pointsPrimeTask = 100;
            config.configVersion = 16;
        }
        // Version 17: the seasonal tasks doubled too, 1 000 points each, unless an admin had set their own.
        if (config.configVersion < 17) {
            if (config.pointsSeasonal == 500) config.pointsSeasonal = 1000;
            config.configVersion = 17;
        }
        config.save();
        return config;
    }

    /**
     * The quest that opens {@code block}, or 0 for none. The quest stage table (Moderation → Settings → Leaderboards)
     * decides it when it has a stage for the block: block N (N ≥ 1) opens with the N-th stage from the bottom, so the
     * pass, the daily ingots and the stage board all follow the same quests. Past the table's end, and for block 0,
     * the pass's own {@link #gates} still count.
     */
    public long gate(int block) {
        List<com.roll_54.roll_mod.minestar.leaderboard.LeaderboardChapters.Stage> stages =
                com.roll_54.roll_mod.minestar.leaderboard.LeaderboardChapters.configured();
        if (block >= 1 && block <= stages.size()) return stages.get(stages.size() - block).chapter();
        return ownGate(block);
    }

    /** The pass's own setting for {@code block}, ignoring the stage table. */
    public long ownGate(int block) {
        if (block < 0 || block >= gates.size()) return 0;
        String hex = gates.get(block);
        if (hex == null || hex.isBlank()) return 0;
        try {
            return Long.parseUnsignedLong(hex.strip(), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static JsonArray strings(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    @SuppressWarnings("unused")
    private static String text(JsonElement e) {
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }
}
