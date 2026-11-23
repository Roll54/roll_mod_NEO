package com.roll_54.roll_mod.minestar.dailytasks;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import com.roll_54.roll_mod.network.packet.BattlePassPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * What the daily tasks pay, in {@code minestar/daily_rewards.json}: the weighted pool two rewards are drawn from per
 * task, and the ingot that replaces iron at each quest stage past the first. Edited from the Moderation tab (Dailies);
 * the server sends its copy to every client, which resolves the rows' rewards against it.
 */
public final class DailyRewardConfig {

    /**
     * One drawable reward: a stable id, {@code item*count}, its weight against the others, and what it turns into from
     * each quest stage on ({@code stages[i]} for stage i+1; blank keeps what it was).
     */
    public static final class Entry {
        public String id = "";
        public String item = "";
        public int weight = 1;
        public List<String> stages = new ArrayList<>();

        public Entry() {}

        Entry(String id, String item, int weight, List<String> stages) {
            this.id = id;
            this.item = item;
            this.weight = weight;
            this.stages = new ArrayList<>(stages);
        }

        String[] row() {
            return new String[]{id, item, String.valueOf(weight), String.join(",", stages == null ? List.of() : stages)};
        }
    }

    public List<Entry> pool = new ArrayList<>();

    /**
     * One all-tasks-done bonus: an id (a code-defined reward's id takes its place), its weight in the daily draw
     * (0 keeps it but never draws it), and what it pays, one line each: {@code namespace:item*min-max} or
     * {@code money:amount}. See {@link com.roll_54.roll_mod.minestar.dailytasks.rewards.ConfiguredBonusReward}.
     */
    public static final class BonusEntry {
        public String id = "";
        public int weight = 1;
        public List<String> payout = new ArrayList<>();

        public BonusEntry() {}

        BonusEntry(String id, int weight, List<String> payout) {
            this.id = id;
            this.weight = weight;
            this.payout = new ArrayList<>(payout);
        }

        String row() {
            return id + "|" + weight + "|" + String.join(";", payout == null ? List.of() : payout);
        }
    }

    /** The daily bonus draw; {@code null} in an older file, which gets the code-defined bonuses written out. */
    public List<BonusEntry> bonus;

    /** What the code-defined bonuses paid, as the first configured list. */
    private static List<BonusEntry> defaultBonus() {
        List<BonusEntry> out = new ArrayList<>();
        out.add(new BonusEntry("diamonds", 1, List.of("minecraft:diamond*8")));
        out.add(new BonusEntry("meteorite_metal", 1, List.of("roll_mod:meteorite_metal_ingot*1-4")));
        out.add(new BonusEntry("prospectors_cache", 1, List.of("minecraft:iron_ingot*8-16", "minecraft:gold_ingot*4-8",
                "minecraft:diamond*1-3", "minecraft:experience_bottle*6")));
        out.add(new BonusEntry("starcoins", 1, List.of("money:2500")));
        return out;
    }
    /**
     * Deprecated: the one list of ingots that replaced iron per stage, from before each reward had stages of its own.
     * Read once into the iron entry's stages; kept so an older file still loads.
     */
    public List<String> stageIngots = new ArrayList<>();

    private static DailyRewardConfig current;

    private static Path file() {
        return MinestarFiles.resolve("daily_rewards.json");
    }

    public static synchronized DailyRewardConfig get() {
        if (current == null) current = load();
        return current;
    }

    private static DailyRewardConfig load() {
        JsonObject json = MinestarFiles.readObject(file());
        DailyRewardConfig config = json.size() == 0 ? null : MinestarFiles.GSON.fromJson(json, DailyRewardConfig.class);
        if (config == null) config = new DailyRewardConfig();
        if (config.pool == null || config.pool.isEmpty()) {
            config.pool = new ArrayList<>();
            for (String[] row : TaskRewardPool.defaultRows()) {
                config.pool.add(new Entry(row[0], row[1], Integer.parseInt(row[2]),
                        row[3].isBlank() ? List.of() : List.of(row[3].split(",", -1))));
            }
        }
        for (Entry e : config.pool) if (e.stages == null) e.stages = new ArrayList<>();
        if (config.bonus == null) config.bonus = defaultBonus();
        for (BonusEntry b : config.bonus) if (b.payout == null) b.payout = new ArrayList<>();
        // An older file's one ingot list becomes the iron entry's own stages.
        if (config.stageIngots != null && !config.stageIngots.isEmpty()) {
            for (Entry e : config.pool) {
                if ("iron_ingots".equals(e.id) && e.stages.isEmpty()) e.stages = new ArrayList<>(config.stageIngots);
            }
            config.stageIngots = new ArrayList<>();
        }
        config.save();
        config.applyToPool();
        return config;
    }

    public synchronized void save() {
        MinestarFiles.write(file(), MinestarFiles.GSON.toJsonTree(this));
    }

    void applyToPool() {
        List<String[]> rows = new ArrayList<>();
        for (Entry e : pool) rows.add(e.row());
        TaskRewardPool.apply(rows);
        applyBonus(bonus == null ? List.of() : bonus);
    }

    private static void applyBonus(List<BonusEntry> entries) {
        List<com.roll_54.roll_mod.minestar.dailytasks.rewards.ConfiguredBonusReward> rewards = new ArrayList<>();
        for (BonusEntry b : entries) {
            if (b.id == null || b.id.isBlank()) continue;
            rewards.add(new com.roll_54.roll_mod.minestar.dailytasks.rewards.ConfiguredBonusReward(
                    b.id.strip(), Math.max(0, b.weight), b.payout == null ? List.of() : b.payout));
        }
        DailyRewardRegistry.applyConfigured(rewards);
    }

    /** The bonus rows of a synced tag: {@code id|weight|line;line}. */
    public static List<BonusEntry> bonusRows(CompoundTag tag) {
        List<BonusEntry> out = new ArrayList<>();
        ListTag list = tag.getList("bonus", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String[] parts = list.getString(i).split("\\|", -1);
            if (parts.length != 3 || parts[0].isBlank()) continue;
            int weight;
            try {
                weight = Math.max(0, Integer.parseInt(parts[1].strip()));
            } catch (NumberFormatException e) {
                weight = 1;
            }
            List<String> payout = new ArrayList<>();
            for (String line : parts[2].split(";", -1)) if (!line.isBlank()) payout.add(line.strip());
            out.add(new BonusEntry(parts[0].strip(), weight, payout));
        }
        return out;
    }

    /** One bonus as a tag row, for the editor. */
    public static String bonusRow(String id, int weight, List<String> payout) {
        return new BonusEntry(id, weight, payout).row();
    }

    /* ----------------------------------------------------- sync ------------------------------------------------------ */

    /** The pool as a packet: {@code dailyPool=true}, rows of {@code id|item|weight}, and the stage ingots. */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("dailyPool", true);
        ListTag rows = new ListTag();
        for (Entry e : pool) rows.add(StringTag.valueOf(String.join("|", e.row())));
        tag.put("rows", rows);
        ListTag bonusRows = new ListTag();
        if (bonus != null) for (BonusEntry b : bonus) bonusRows.add(StringTag.valueOf(b.row()));
        tag.put("bonus", bonusRows);
        return tag;
    }

    /** A client's (or the editor's) copy from {@link #toTag}. */
    public static void applyTag(CompoundTag tag) {
        TaskRewardPool.apply(rows(tag));
        applyBonus(bonusRows(tag));
    }

    public static List<String[]> rows(CompoundTag tag) {
        List<String[]> rows = new ArrayList<>();
        ListTag list = tag.getList("rows", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String[] parts = list.getString(i).split("\\|", -1);
            if (parts.length == 3) rows.add(new String[]{parts[0], parts[1], parts[2], ""});
            else if (parts.length == 4) rows.add(parts);
        }
        return rows;
    }

    public static List<String> strings(CompoundTag tag) {
        List<String> out = new ArrayList<>();
        ListTag list = tag.getList("stages", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BattlePassPackets.Data(get().toTag()));
    }

    /** From the Moderation tab: replaces the pool and the stage ingots, saves, and tells everyone online. */
    public static void applyAdmin(ServerPlayer player, CompoundTag args) {
        if (!com.roll_54.roll_mod.minestar.moderation.ModerationPermissions.canBan(player)) return;
        DailyRewardConfig config = get();
        List<Entry> pool = new ArrayList<>();
        for (String[] row : rows(args)) {
            if (row[0].isBlank() || TaskRewardPool.parse(row[1]).isEmpty()) continue;
            int weight;
            try {
                weight = Math.max(1, Integer.parseInt(row[2].strip()));
            } catch (NumberFormatException e) {
                weight = 1;
            }
            List<String> stages = new ArrayList<>(row[3].isBlank() ? List.of() : List.of(row[3].split(",", -1)));
            // Only items: a stage that names nothing known is kept blank (stays as it was).
            stages.replaceAll(st -> TaskRewardPool.parse(st).isEmpty() ? "" : st.strip());
            while (!stages.isEmpty() && stages.get(stages.size() - 1).isBlank()) stages.remove(stages.size() - 1);
            pool.add(new Entry(row[0].strip(), row[1].strip(), weight, stages));
        }
        if (pool.size() < TaskRewardPool.NUM_PER_TASK) return; // a task draws two; fewer would leave rows half paid
        config.pool = pool;
        // The bonus list, when the editor sent one (an older client sends only the pool).
        if (args.contains("bonus")) {
            List<BonusEntry> bonus = new ArrayList<>();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (BonusEntry b : bonusRows(args)) {
                if (!b.id.matches("[a-z0-9_]+") || !seen.add(b.id)) continue;
                bonus.add(b);
            }
            config.bonus = bonus;
        }
        config.save();
        config.applyToPool();
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) sync(p);
    }
}
