package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.minestar.dailytasks.api.TaskReward;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * What finishing one daily task pays: {@link #NUM_PER_TASK} rewards drawn from {@link #all()},
 * plus {@link #STARCOINS} every time.
 *
 * <p>The table below is the only place payouts are tuned — amounts and weights are meant to be
 * edited, and a new entry is one more line.
 *
 * <p>Which rewards a given row draws is not stored anywhere: {@code DailyTaskManager.rewardsFor}
 * derives it from the same seed the daily roll uses, so it is the same on every read and across a
 * restart. The screen syncs a reward's <em>index in {@link #all()}</em>, so the order here is
 * load-bearing in the same way {@code DailyRewardRegistry}'s is — client and server run the same
 * jar, so they agree, but an index must never be persisted.
 */
public final class TaskRewardPool {

    /** How many pool rewards one task pays, on top of {@link #STARCOINS}. */
    public static final int NUM_PER_TASK = 2;

    /**
     * Weights are relative to each other: the total below is 25, so an iron payout comes up on
     * roughly one draw in six and netherite on one in twenty-five.
     */
    /**
     * The defaults written to {@code minestar/daily_rewards.json} the first time: {@code id, item*count, weight}. The
     * file — and the Moderation tab's Dailies editor, which edits it — is what is used from then on.
     */
    static final String[][] DEFAULT_POOL = {
            {"iron_ingots", "minecraft:iron_ingot*4", "4"},
            {"copper_ingots", "minecraft:copper_ingot*18", "4"},
            {"redstone", "roll_mod:redstone_dust*32", "3"},
            {"lapis", "minecraft:lapis_lazuli*24", "3"},
            {"amethyst", "minecraft:amethyst_shard*12", "3"},
            {"gold_ingots", "minecraft:gold_ingot*8", "2"},
            {"emeralds", "minecraft:emerald*6", "2"},
            {"ender_pearls", "minecraft:ender_pearl*4", "2"},
            {"diamonds", "minecraft:diamond*8", "1"},
            {"netherite_ingots", "minecraft:netherite_ingot*2", "1"}};

    /**
     * Per quest stage past the first, the ingot that stands in for iron: the battle pass's own ingots, none past MV
     * (bronze, steel, then aluminium and electrum).
     */
    static final String[] DEFAULT_STAGES = {
            "modern_industrialization:bronze_ingot*12", "modern_industrialization:steel_ingot*10",
            "modern_industrialization:steel_ingot*16", "modern_industrialization:aluminum_ingot*10",
            "modern_industrialization:aluminum_ingot*16", "modern_industrialization:electrum_ingot*12",
            "modern_industrialization:electrum_ingot*16"};

    /** What is drawn now: from the file on the server, from the server's copy on a client. */
    private static List<TaskReward> pool;
    /** The pool per quest stage, 0 … {@link #STAGES}. */
    private static List<List<TaskReward>> byStage;
    private static List<TaskReward> all;

    /** What {@link #STARCOINS} pays per task; also the number its name shows on hover. */
    public static final long STARCOIN_AMOUNT = 2500;

    /**
     * Paid on top of the draw, every task, every time — so it is deliberately not in the pool and
     * cannot be drawn twice. The icon is a PNG, so it carries an item for the advancement toast,
     * which can only render a stack.
     */
    public static final TaskReward STARCOINS = new TaskReward.Currency(
            "starcoins",
            // Lowercase, and it has to stay that way: a ResourceLocation path may only hold
            // [a-z0-9/._-], and an uppercase one throws out of this static initialiser.
            DailyTaskIcon.of(RollMod.id("textures/item/lp.png"), Items.GOLD_NUGGET),
            Component.translatable("gui.roll_mod.daily_tasks.reward.starcoins", STARCOIN_AMOUNT),
            CurrencyType.MAIN,
            STARCOIN_AMOUNT,
            1);

    private TaskRewardPool() {}

    /** roll_mod's own redstone dust, which the rest of the pack uses; vanilla redstone only if it is missing. */
    private static ItemLike rollRedstone() {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.roll_54.roll_mod.RollMod.id("redstone_dust"));
        return item == Items.AIR ? Items.REDSTONE : item;
    }

    private static TaskReward stack(String id, ItemLike item, int count, int weight) {
        return new TaskReward.Stack(id, new ItemStack(item, count), weight);
    }

    /** Quest stages past the first a reward can change at (bronze … IV). */
    public static final int STAGES = 7;

    /**
     * Builds the pool from rows of {@code id, item*count, weight, stages}: {@code stages} is a comma list of up to
     * {@link #STAGES} items the reward turns into from that quest stage on (blank: it stays as it was). Every variant
     * gets an id of its own, so a row's synced index always names exactly what it pays.
     */
    public static synchronized void apply(List<String[]> rows) {
        List<List<TaskReward>> levels = new ArrayList<>();
        for (int st = 0; st <= STAGES; st++) levels.add(new ArrayList<>());
        java.util.LinkedHashMap<String, TaskReward> everything = new java.util.LinkedHashMap<>();
        for (String[] row : rows) {
            if (parse(row[1]).isEmpty()) continue;
            int weight;
            try {
                weight = Math.max(1, Integer.parseInt(row[2].strip()));
            } catch (NumberFormatException e) {
                weight = 1;
            }
            String[] changes = row.length > 3 && !row[3].isBlank() ? row[3].split(",", -1) : new String[0];
            String current = row[1].strip();
            for (int st = 0; st <= STAGES; st++) {
                if (st >= 1 && st - 1 < changes.length && !parse(changes[st - 1]).isEmpty()) current = changes[st - 1].strip();
                String spec = current;
                String id = spec.equals(row[1].strip()) ? row[0] : row[0] + "@" + spec;
                int w = weight;
                levels.get(st).add(everything.computeIfAbsent(id, k -> reward(k, spec, w)));
            }
        }
        byStage = new ArrayList<>();
        for (List<TaskReward> level : levels) byStage.add(List.copyOf(level));
        pool = byStage.get(0);
        all = List.copyOf(everything.values());
    }

    /** The built-in rows: the defaults, the iron ingots turning into the battle pass's ingots stage by stage. */
    static List<String[]> defaultRows() {
        List<String[]> rows = new ArrayList<>();
        for (String[] row : DEFAULT_POOL) {
            rows.add(new String[]{row[0], row[1], row[2], "iron_ingots".equals(row[0]) ? String.join(",", DEFAULT_STAGES) : ""});
        }
        return rows;
    }

    /**
     * The built-in defaults until something better arrives: the server's file (read when the server first needs it, see
     * {@link DailyRewardConfig#get}) or, on a client, the server's copy. A client never reads or writes the file itself.
     */
    private static synchronized void ensure() {
        if (all == null) apply(defaultRows());
    }

    /** A row's payout: a stack, or a range rolled at claim time for {@code item*min-max}. */
    private static TaskReward reward(String id, String spec, int weight) {
        ItemStack stack = parse(spec);
        int[] range = range(spec);
        if (range[0] == range[1]) return new TaskReward.Stack(id, stack.copyWithCount(range[0]), weight);
        return new TaskReward.Range(id, com.roll_54.roll_mod.minestar.dailytasks.api.RewardRange.of(
                stack.getItemHolder(), range[0], range[1]), weight);
    }

    /** {@code {min, max}} of {@code item*count} or {@code item*min-max}; no count is one. */
    static int[] range(String spec) {
        int star = spec == null ? -1 : spec.lastIndexOf('*');
        if (star < 0) return new int[]{1, 1};
        String amount = spec.substring(star + 1).strip();
        int dash = amount.indexOf('-');
        int min = count(dash < 0 ? amount : amount.substring(0, dash));
        int max = dash < 0 ? min : Math.max(min, count(amount.substring(dash + 1)));
        return new int[]{min, max};
    }

    private static int count(String text) {
        try {
            return Math.max(1, Integer.parseInt(text.strip()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** {@code namespace:item*count} ({@code *min-max} reads as its lower end), or empty for an unknown item. */
    static ItemStack parse(String spec) {
        if (spec == null) return ItemStack.EMPTY;
        int star = spec.lastIndexOf('*');
        String id = (star < 0 ? spec : spec.substring(0, star)).strip();
        int count = range(spec)[0];
        var rl = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (rl == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(rl)) return ItemStack.EMPTY;
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl), count);
    }

    /** The drawable pool and the stage ingots, in the order the screen's indices refer to. */
    public static synchronized List<TaskReward> all() {
        ensure();
        return all;
    }

    private static synchronized List<TaskReward> pool() {
        ensure();
        return pool;
    }

    /** The pool as it pays at quest stage {@code stage} (0 before the first). */
    private static synchronized List<TaskReward> poolAt(int stage) {
        ensure();
        return byStage.get(Math.max(0, Math.min(STAGES, stage)));
    }

    /** {@code -1} for an id the pool no longer holds, which the screen renders as no icon. */
    public static int indexOf(String id) {
        List<TaskReward> list = all();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(id)) return i;
        }
        return -1;
    }

    /** {@code null} outside the pool, including for the {@code -1} above. */
    @Nullable
    public static TaskReward byIndex(int index) {
        List<TaskReward> list = all();
        return index < 0 || index >= list.size() ? null : list.get(index);
    }

    /**
     * Draws {@code count} rewards by weight, <em>without</em> replacement — one task never pays the
     * same thing twice. Returns fewer only if the pool itself is smaller than {@code count}.
     */
    public static List<TaskReward> draw(Random rng, int count) {
        return draw(rng, count, 0);
    }

    /**
     * As {@link #draw(Random, int)}, scaled to the quest stage: past the first stage (bronze onwards) the iron ingots
     * become that stage's ingot.
     */
    public static List<TaskReward> draw(Random rng, int count, int stage) {
        // Each reward as it is at this stage (iron becomes the stage's ingot, and whatever else the editor set).
        List<TaskReward> remaining = new ArrayList<>(poolAt(stage));
        List<TaskReward> picked = new ArrayList<>(count);

        while (picked.size() < count && !remaining.isEmpty()) {
            int total = 0;
            for (TaskReward reward : remaining) {
                total += Math.max(1, reward.weight()); // a mistyped weight must not break the draw
            }

            int roll = rng.nextInt(total);
            for (Iterator<TaskReward> it = remaining.iterator(); it.hasNext(); ) {
                TaskReward reward = it.next();
                roll -= Math.max(1, reward.weight());
                if (roll < 0) {
                    picked.add(reward);
                    it.remove();
                    break;
                }
            }
        }

        return List.copyOf(picked);
    }
}
