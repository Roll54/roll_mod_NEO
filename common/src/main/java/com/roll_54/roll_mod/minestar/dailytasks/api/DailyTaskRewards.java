package com.roll_54.roll_mod.minestar.dailytasks.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared payout helpers for {@link DailyTask#grantReward(ServerPlayer)}. */
public final class DailyTaskRewards {

    private DailyTaskRewards() {}

    /**
     * TODO: replace with the real reward table. Everything around the payout — completion tracking,
     * the per-player claim set, the Claim button and its validation — is already wired up; only
     * what a task hands over is a stand-in. Override {@link DailyTask#grantReward} on a task to
     * give something specific.
     */
    public static ItemStack placeholder() {
        return new ItemStack(Items.DIAMOND, 1);
    }

    /** Puts the stack in the player's inventory, dropping it at their feet if there is no room. */
    public static void give(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
    }
}
