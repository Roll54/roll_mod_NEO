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
        /** What it pays at the start, a line each; a blank line pays nothing until a stage gives it something. */
        public List<String> payout = new ArrayList<>();
        /**
         * Per quest stage past the first (bronze … IV), what the bonus pays from that stage on instead; an empty list
         * keeps what the stage before paid.
         * @deprecated each payout line has stages of its own now ({@link #lineStages}); an older file's stages are read
         * into them once ({@link #migrate}) and this is left empty. Kept so such a file still loads.
         */
        @Deprecated
        public List<List<String>> stages = new ArrayList<>();
        /**
         * Per payout line (same index as {@link #payout}), what that line pays from each quest stage on (bronze … IV,
         * {@link TaskRewardPool#STAGES} of them): blank keeps what the stage before paid, {@link #NONE} stops paying it.
         */
        public List<List<String>> lineStages = new ArrayList<>();

        public BonusEntry() {}

        BonusEntry(String id, int weight, List<String> payout) {
            this.id = id;
            this.weight = weight;
            this.payout = new ArrayList<>(payout);
        }

        /** With per-line stages: {@code lineStages.get(i)} is line {@code i}'s value at each stage. */
        public static BonusEntry withLineStages(String id, int weight, List<String> payout, List<List<String>> lineStages) {
            BonusEntry entry = new BonusEntry(id, weight, payout);
            for (List<String> per : lineStages) entry.lineStages.add(new ArrayList<>(per));
            entry.normalize();
            return entry;
        }

        /**
         * @deprecated with whole-payout stages, the old shape; read into per-line stages at once.
         */
        @Deprecated
        public BonusEntry(String id, int weight, List<String> payout, List<List<String>> stages) {
            this(id, weight, payout);
            for (List<String> st : stages) this.stages.add(new ArrayList<>(st));
            migrate();
        }

        /**
         * An older entry's whole-payout stages into per-line ones. A stage that listed lines paid exactly those: line
         * {@code i} takes that stage's line {@code i}, a line the stage left out stops ({@link #NONE}), and a stage
         * with more lines than the start adds lines that pay nothing before it. Nothing is lost.
         */
        void migrate() {
            if (payout == null) payout = new ArrayList<>();
            if (lineStages == null) lineStages = new ArrayList<>();
            if (stages != null && !stages.isEmpty() && lineStages.isEmpty()) {
                int lines = payout.size();
                for (List<String> st : stages) if (st != null) lines = Math.max(lines, st.size());
                while (payout.size() < lines) payout.add("");
                for (int i = 0; i < lines; i++) lineStages.add(new ArrayList<>());
                for (int s = 0; s < stages.size() && s < TaskRewardPool.STAGES; s++) {
                    List<String> st = stages.get(s);
                    for (int i = 0; i < lines; i++) {
                        String value = "";
                        if (st != null && !st.isEmpty()) {
                            String own = i < st.size() && st.get(i) != null ? st.get(i).strip() : "";
                            String before = lineAt(i, s);
                            String now = own.isEmpty() ? NONE : own;
                            // Only where it changes: a stage that repeats what came before stays blank.
                            if (!(now.equals(NONE) ? before.isEmpty() : now.equals(before))) value = now;
                        }
                        lineStages.get(i).add(value);
                    }
                }
                stages = new ArrayList<>();
            }
            if (stages == null) stages = new ArrayList<>();
            normalize();
        }

        /** One stage list per payout line, each {@link TaskRewardPool#STAGES} long. */
        void normalize() {
            if (payout == null) payout = new ArrayList<>();
            if (lineStages == null) lineStages = new ArrayList<>();
            while (lineStages.size() < payout.size()) lineStages.add(new ArrayList<>());
            while (lineStages.size() > payout.size()) lineStages.remove(lineStages.size() - 1);
            for (int i = 0; i < lineStages.size(); i++) {
                List<String> per = lineStages.get(i) == null ? new ArrayList<>() : new ArrayList<>(lineStages.get(i));
                per.replaceAll(v -> v == null ? "" : v.strip());
                while (per.size() < TaskRewardPool.STAGES) per.add("");
                while (per.size() > TaskRewardPool.STAGES) per.remove(per.size() - 1);
                lineStages.set(i, per);
            }
        }

        /** What line {@code i} pays at quest stage {@code stage} (0 the start, 1 … STAGES); "" for nothing. */
        public String lineAt(int i, int stage) {
            String value = i < payout.size() && payout.get(i) != null ? payout.get(i).strip() : "";
            List<String> per = i < lineStages.size() && lineStages.get(i) != null ? lineStages.get(i) : List.of();
            for (int s = 1; s <= stage && s - 1 < per.size(); s++) {
                String own = per.get(s - 1);
                if (own != null && !own.isBlank()) value = own.strip();
            }
            return NONE.equals(value) ? "" : value;
        }

        /** Everything it pays at {@code stage}, blank lines left out. */
        public List<String> payoutAt(int stage) {
            List<String> out = new ArrayList<>();
            for (int i = 0; i < payout.size(); i++) {
                String line = lineAt(i, stage);
                if (!line.isEmpty()) out.add(line);
            }
            return out;
        }

        /** {@code id|weight|line;line||line1stage,…~line2stage,…}: the old stages part stays empty, positions kept. */
        String row() {
            normalize();
            List<String> per = new ArrayList<>();
            for (List<String> l : lineStages) per.add(String.join(",", l));
            return id + "|" + weight + "|" + String.join(";", payout) + "||" + String.join("~", per);
        }
    }

    /** A stage value that stops a bonus line from paying. */
    public static final String NONE = "none";

    /** Per configured bonus id, the reward id it pays at each quest stage 0 … {@link TaskRewardPool#STAGES}. */
    private static volatile java.util.Map<String, String[]> bonusByStage = java.util.Map.of();

    /** The reward the bonus drawn as {@code id} pays at {@code stage}: its stage variant, or {@code id} itself. */
    public static String bonusAt(String id, int stage) {
        String[] ids = bonusByStage.get(id);
        if (ids == null) return id;
        return ids[Math.max(0, Math.min(ids.length - 1, stage))];
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
        // An older file's whole-payout stages become each line's own (BonusEntry.migrate).
        for (BonusEntry b : config.bonus) b.migrate();
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

    /**
     * The bonuses into the registry: each one as drawn, plus a variant ({@code id@sN}, never drawn itself) from every
     * stage that changes what it pays. The draw picks the bonus; {@link #bonusAt} turns it into what the group's stage
     * pays, for the panel and the claim alike.
     */
    private static void applyBonus(List<BonusEntry> entries) {
        List<com.roll_54.roll_mod.minestar.dailytasks.rewards.ConfiguredBonusReward> rewards = new ArrayList<>();
        java.util.Map<String, String[]> byStage = new java.util.HashMap<>();
        for (BonusEntry b : entries) {
            if (b.id == null || b.id.isBlank()) continue;
            String id = b.id.strip();
            b.migrate();
            rewards.add(new com.roll_54.roll_mod.minestar.dailytasks.rewards.ConfiguredBonusReward(
                    id, Math.max(0, b.weight), b.payoutAt(0)));
            String[] ids = new String[TaskRewardPool.STAGES + 1];
            ids[0] = id;
            // Each line follows its own stages: a stage where any line changes is a variant with every line as it
            // stands there; one where nothing changes pays as the stage before.
            for (int st = 1; st <= TaskRewardPool.STAGES; st++) {
                List<String> now = b.payoutAt(st);
                if (!now.equals(b.payoutAt(st - 1))) {
                    String variant = id + "@s" + st;
                    rewards.add(com.roll_54.roll_mod.minestar.dailytasks.rewards.ConfiguredBonusReward.variant(variant, now));
                    ids[st] = variant;
                } else {
                    ids[st] = ids[st - 1];
                }
            }
            byStage.put(id, ids);
        }
        bonusByStage = java.util.Map.copyOf(byStage);
        DailyRewardRegistry.applyConfigured(rewards);
    }

    /** The bonus rows of a synced tag: {@code id|weight|line;line} and, after another {@code |}, the stages. */
    public static List<BonusEntry> bonusRows(CompoundTag tag) {
        List<BonusEntry> out = new ArrayList<>();
        ListTag list = tag.getList("bonus", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String[] parts = list.getString(i).split("\\|", -1);
            if (parts.length < 3 || parts.length > 5 || parts[0].isBlank()) continue;
            int weight;
            try {
                weight = Math.max(0, Integer.parseInt(parts[1].strip()));
            } catch (NumberFormatException e) {
                weight = 1;
            }
            List<String> payout = new ArrayList<>();
            if (parts.length == 5) {
                // Per-line stages: line positions matter, so a blank line (paid only from a stage on) is kept.
                if (!parts[2].isEmpty()) for (String line : parts[2].split(";", -1)) payout.add(line.strip());
                List<List<String>> lineStages = new ArrayList<>();
                if (!parts[4].isEmpty()) {
                    for (String per : parts[4].split("~", -1)) lineStages.add(new ArrayList<>(List.of(per.split(",", -1))));
                }
                out.add(BonusEntry.withLineStages(parts[0].strip(), weight, payout, lineStages));
                continue;
            }
            for (String line : parts[2].split(";", -1)) if (!line.isBlank()) payout.add(line.strip());
            List<List<String>> stages = new ArrayList<>();
            if (parts.length == 4 && !parts[3].isBlank()) {
                for (String st : parts[3].split("~", -1)) {
                    List<String> lines = new ArrayList<>();
                    for (String line : st.split(";", -1)) if (!line.isBlank()) lines.add(line.strip());
                    stages.add(lines);
                }
            }
            out.add(new BonusEntry(parts[0].strip(), weight, payout, stages));
        }
        return out;
    }

    /** One bonus as a tag row, for the editor. */
    public static String bonusRow(String id, int weight, List<String> payout) {
        return new BonusEntry(id, weight, payout).row();
    }

    /** @deprecated with whole-payout stages; see {@link #bonusRowByLine}. */
    @Deprecated
    public static String bonusRow(String id, int weight, List<String> payout, List<List<String>> stages) {
        return new BonusEntry(id, weight, payout, stages).row();
    }

    /** One bonus as a tag row, with each line's own stages. */
    public static String bonusRowByLine(String id, int weight, List<String> payout, List<List<String>> lineStages) {
        return BonusEntry.withLineStages(id, weight, payout, lineStages).row();
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
