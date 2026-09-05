package com.roll_54.roll_mod.minestar.dailytasks.api;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shared payout helpers, used by both {@link DailyTask#grantReward(ServerPlayer)} and
 * {@link DailyReward#grant(ServerPlayer)}.
 */
public final class DailyTaskRewards {

    private DailyTaskRewards() {}

    /**
     * TODO: replace with the real reward table. Everything around the payout — completion tracking,
     * the per-player claim set, the Claim button and its validation — is already wired up; only
     * what a task hands over is a stand-in. Override {@link DailyTask#grantReward} on a task to
     * give something specific.
     *
     * <p>This is the <em>per-task</em> stand-in only. The all-complete bonus has a real reward
     * table — see {@link DailyReward}.
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

    /**
     * Runs a command from the console on the player's behalf. {@code %player%} in the template is
     * replaced with their name, so a reward can be written as e.g.
     * {@code "give %player% minecraft:diamond 8"}.
     *
     * <p>Runs at permission level 4 — a reward is server-authored, not player input — and swallows
     * failures with a log line rather than letting one bad command break the claim that triggered
     * it. Same shape as the shop's delivery commands.
     *
     * @return whether the command ran without throwing
     */
    public static boolean runCommand(ServerPlayer player, String template) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        String command = template.replace("%player%", player.getGameProfile().getName());
        try {
            CommandSourceStack source = server.createCommandSourceStack().withPermission(4);
            server.getCommands().performPrefixedCommand(source, command);
            return true;
        } catch (Exception e) {
            RollMod.LOGGER.error("[DailyTasks] Reward command failed: {}", command, e);
            return false;
        }
    }
}
