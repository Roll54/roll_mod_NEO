package com.roll_54.roll_mod.minestar.dailytasks.api;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * "Between {@code min} and {@code max} of this item" — one payout, rolled and drawn from the same
 * pair of numbers.
 *
 * <p>The point of the record is that those two numbers are written once. A reward that rolls its
 * amount has to advertise what it can pay <em>before</em> it pays it, so the range reaches both the
 * roll in {@link DailyReward#grant(ServerPlayer)} and the caption on the reward strip; keeping them
 * in one value is what stops the strip promising 1-4 while the roll hands out 1-6.
 *
 * <pre>{@code
 * private static final RewardRange INGOTS = RewardRange.of(ItemRegistry.METEORITE_METAL_INGOT, 1, 4);
 *
 * @Override public List<DailyTaskIcon> icons() { return List.of(INGOTS.icon()); }
 * @Override public void grant(ServerPlayer player) { INGOTS.grant(player); }
 * }</pre>
 *
 * <p>Holds a {@link Holder} rather than an {@link Item}, so a {@code DeferredHolder} constant can
 * be handed straight over and is only dereferenced when the reward is actually drawn or paid.
 *
 * @param min the smallest amount the roll can produce, at least 1
 * @param max the largest, at least {@code min}; equal to {@code min} for a fixed amount
 */
public record RewardRange(Holder<Item> item, int min, int max) {

    public RewardRange {
        if (min < 1) {
            throw new IllegalArgumentException("Reward range minimum must be at least 1, got " + min);
        }
        if (max < min) {
            throw new IllegalArgumentException("Reward range " + min + "-" + max + " is inverted");
        }
    }

    /** A rolled amount of a registered item, e.g. {@code of(ItemRegistry.FOO, 1, 4)}. */
    public static RewardRange of(Holder<Item> item, int min, int max) {
        return new RewardRange(item, min, max);
    }

    /** The same for a vanilla item or a block's item form. */
    public static RewardRange of(ItemLike item, int min, int max) {
        return new RewardRange(BuiltInRegistries.ITEM.wrapAsHolder(item.asItem()), min, max);
    }

    /** A fixed amount — a range whose ends meet, so a payout list can hold both kinds. */
    public static RewardRange of(Holder<Item> item, int count) {
        return new RewardRange(item, count, count);
    }

    /** A fixed amount of a vanilla item. */
    public static RewardRange of(ItemLike item, int count) {
        return new RewardRange(BuiltInRegistries.ITEM.wrapAsHolder(item.asItem()), count, count);
    }

    /** How much this payout comes to this time. Uniform over the whole range, both ends included. */
    public int roll(RandomSource random) {
        return min == max ? min : random.nextIntBetweenInclusive(min, max);
    }

    /** {@link #roll} as a stack, ready to hand over. */
    public ItemStack rollStack(RandomSource random) {
        return new ItemStack(item, roll(random));
    }

    /**
     * Rolls and pays, dropping at the player's feet if the inventory is full. Uses the player's own
     * {@link RandomSource}, so the amount is decided server-side at claim time — {@code grant} runs
     * once per party member, and each of them rolls their own.
     */
    public void grant(ServerPlayer player) {
        DailyTaskRewards.give(player, rollStack(player.getRandom()));
    }

    /**
     * The item on its own, with no amount on it — what a reward hands back from
     * {@link DailyReward#icon()}, the emblem the bonus panel draws above its Collect button. The
     * amount belongs on the strip underneath it, which draws {@link #icon()}; putting the range in
     * both places says the same thing twice on one panel.
     */
    public DailyTaskIcon itemIcon() {
        return DailyTaskIcon.of(new ItemStack(item));
    }

    /**
     * What the reward strip draws for this payout: the item, captioned with the range rather than
     * with a count it cannot know yet. A fixed amount draws as an ordinary stack instead, count and
     * all, so it looks like every other fixed payout on the screen.
     */
    public DailyTaskIcon icon() {
        return min == max
                ? DailyTaskIcon.of(new ItemStack(item, min))
                : DailyTaskIcon.ofRange(new ItemStack(item), min, max);
    }
}
