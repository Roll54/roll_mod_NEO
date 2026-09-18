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
    private static final List<TaskReward> POOL = List.of(
            stack("iron_ingots", Items.IRON_INGOT, 4, 4),
            stack("copper_ingots", Items.COPPER_INGOT, 18, 4),
            stack("redstone", Items.REDSTONE, 32, 3),
            stack("lapis", Items.LAPIS_LAZULI, 24, 3),
            stack("amethyst", Items.AMETHYST_SHARD, 12, 3),
            stack("gold_ingots", Items.GOLD_INGOT, 8, 2),
            stack("emeralds", Items.EMERALD, 6, 2),
            stack("ender_pearls", Items.ENDER_PEARL, 4, 2),
            stack("diamonds", Items.DIAMOND, 8, 1),
            stack("netherite_ingots", Items.NETHERITE_INGOT, 2, 1));

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
            Component.translatable("gui.roll_mod.daily_tasks.reward.starcoins"),
            CurrencyType.MAIN,
            250,
            1);

    private TaskRewardPool() {}

    private static TaskReward stack(String id, ItemLike item, int count, int weight) {
        return new TaskReward.Stack(id, new ItemStack(item, count), weight);
    }

    /** The drawable pool, in the order the screen's indices refer to. */
    public static List<TaskReward> all() {
        return POOL;
    }

    /** {@code -1} for an id the pool no longer holds, which the screen renders as no icon. */
    public static int indexOf(String id) {
        for (int i = 0; i < POOL.size(); i++) {
            if (POOL.get(i).id().equals(id)) return i;
        }
        return -1;
    }

    /** {@code null} outside the pool, including for the {@code -1} above. */
    @Nullable
    public static TaskReward byIndex(int index) {
        return index < 0 || index >= POOL.size() ? null : POOL.get(index);
    }

    /**
     * Draws {@code count} rewards by weight, <em>without</em> replacement — one task never pays the
     * same thing twice. Returns fewer only if the pool itself is smaller than {@code count}.
     */
    public static List<TaskReward> draw(Random rng, int count) {
        List<TaskReward> remaining = new ArrayList<>(POOL);
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
