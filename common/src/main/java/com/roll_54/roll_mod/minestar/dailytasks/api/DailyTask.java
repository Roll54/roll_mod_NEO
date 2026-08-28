package com.roll_54.roll_mod.minestar.dailytasks.api;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * One kind of daily task. Implement this, annotate the class with {@link AutoDailyTask}, and the
 * registry picks it up at startup — see {@code DailyTaskRegistry}.
 *
 * <p>Implementations must be stateless: a single instance is shared by every player and every
 * party. All mutable per-group progress lives in {@code DailyTasksState}.
 */
public interface DailyTask {

    /**
     * Stable identifier, e.g. {@code "mine_ores"}. Written into the save file, so renaming one
     * retires the old id and resets progress for anyone mid-task.
     */
    String id();

    /** Which Minecraft event feeds this task, and therefore what {@link #matches} receives. */
    DailyTaskHook hook();

    /**
     * The requirement for a player in no party. The number actually shown is this scaled by team
     * size — see {@code DailyTaskManager.requiredAmount}.
     */
    int baseAmount();

    /**
     * Whether this occurrence counts. {@code subject}'s runtime type is fixed by {@link #hook()};
     * anything else must return {@code false}, so pattern-match rather than cast — {@code
     * instanceof} also covers the null case for free.
     */
    boolean matches(Object subject);

    /**
     * The task's title, shown on its row and on the completion toast. Defaults to the
     * {@link #titleKey()} translation, so a task normally just adds the lang key.
     */
    default Component name() {
        return Component.translatable(titleKey());
    }

    /**
     * The description shown when hovering the row and on the completion toast. Defaults to the
     * {@link #descKey()} translation with {@code required} substituted in.
     *
     * @param required the team-scaled requirement, ready to be substituted into the text
     */
    default Component tooltip(int required) {
        return Component.translatable(descKey(), required);
    }

    /** {@code dailytask.roll_mod.<id>.title}. */
    default String titleKey() {
        return "dailytask." + RollMod.MODID + "." + id() + ".title";
    }

    /** {@code dailytask.roll_mod.<id>.desc}, formatted with the scaled requirement. */
    default String descKey() {
        return "dailytask." + RollMod.MODID + "." + id() + ".desc";
    }

    /** What the row and the toast draw: an item, or a PNG. */
    DailyTaskIcon icon();

    /**
     * Pays out to one player who claimed this task. Progress is shared across a party, but the
     * reward is per player, so this runs once per member who presses Claim.
     *
     * <p>The default is the placeholder payout; override it to give something task-specific.
     */
    default void grantReward(ServerPlayer player) {
        DailyTaskRewards.give(player, DailyTaskRewards.placeholder());
    }

    /**
     * Relative likelihood of being drawn in the daily roll, against other tasks of a different
     * hook. Must be positive; the default weights every task equally.
     */
    default int weight() {
        return 1;
    }

    /** Convenience for {@link #icon()}'s toast stack. */
    default ItemStack toastIcon() {
        return icon().toastStack();
    }
}
