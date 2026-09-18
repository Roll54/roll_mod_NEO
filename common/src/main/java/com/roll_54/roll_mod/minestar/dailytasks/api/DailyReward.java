package com.roll_54.roll_mod.minestar.dailytasks.api;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The payout for clearing a whole day's board — every daily task completed.
 *
 * <p>One reward is drawn per group per day, alongside the task set, and shown in the panel down the
 * left of the daily-tasks screen. What a reward actually <em>does</em> is deliberately unconstrained:
 * {@link #grant} may hand over items, run a console command, change the player's attributes, or
 * anything else. The daily-task core never needs to know which.
 *
 * <p>Implement this, annotate the class with {@link AutoDailyReward}, and the registry picks it up
 * at startup — see {@code DailyRewardRegistry}.
 *
 * <p>Implementations must be stateless: a single instance is shared by every player and every
 * party. All mutable per-group state lives in {@code DailyTasksState}.
 */
public interface DailyReward {

    /**
     * Stable identifier, e.g. {@code "diamonds"}. Written into the save file, so renaming one
     * retires the old id — a group holding it falls back to no bonus until the next daily roll.
     */
    String id();

    /** What the panel draws: an item, or a PNG. Shares {@link DailyTaskIcon} with the task rows. */
    DailyTaskIcon icon();

    /**
     * What the bonus panel's reward strip draws — a declared preview of the payout, not an
     * inspection of it. {@link #grant} is free to do something that is not items at all, so a
     * reward that wants its contents shown says so here; everything else falls back to the single
     * {@link #icon()}.
     *
     * <p>Resolved entirely on the client from its own copy of {@link
     * com.roll_54.roll_mod.minestar.dailytasks.DailyRewardRegistry}, so nothing here crosses the
     * wire.
     *
     * <p>A payout that rolls its amount cannot show a count, so it shows its range instead:
     * {@link RewardRange#icon()} captions the item with e.g. {@code "1-4"}, from the same two
     * numbers the roll uses.
     */
    default List<DailyTaskIcon> icons() {
        return List.of(icon());
    }

    /**
     * Pays out to one player who pressed Collect. Progress is shared across a party, but the payout
     * is per player, so this runs once per member.
     *
     * <p>Called on the server thread with the player online and already validated as eligible.
     */
    void grant(ServerPlayer player);

    /**
     * The reward's title, shown in the panel tooltip. Defaults to the {@link #titleKey()}
     * translation, so a reward normally just adds the lang key.
     */
    default Component name() {
        return Component.translatable(titleKey());
    }

    /** The line under the title in the panel tooltip, describing what the reward gives. */
    default Component tooltip() {
        return Component.translatable(descKey());
    }

    /** {@code dailyreward.roll_mod.<id>.title}. */
    default String titleKey() {
        return "dailyreward." + RollMod.MODID + "." + id() + ".title";
    }

    /** {@code dailyreward.roll_mod.<id>.desc}. */
    default String descKey() {
        return "dailyreward." + RollMod.MODID + "." + id() + ".desc";
    }

    /**
     * Relative likelihood of being drawn in the daily roll. Must be positive; the default weights
     * every reward equally. Unlike the task draw, this one genuinely honours the weight — see
     * {@code DailyTaskManager.pickReward}.
     */
    default int weight() {
        return 1;
    }

    /** Convenience for {@link #icon()}'s toast stack. */
    default ItemStack toastIcon() {
        return icon().toastStack();
    }
}
