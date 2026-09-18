package com.roll_54.roll_mod.minestar.dailytasks.api;

import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * One entry in the per-task reward table — see {@code TaskRewardPool}.
 *
 * <p>Deliberately smaller than {@link DailyReward}: the all-complete bonus is an extension point,
 * where a reward is a class another mod can contribute through an annotation scan. These are a data
 * table instead, so the whole pool is one readable list rather than ten near-identical classes, and
 * the two shapes it needs are the two records below.
 *
 * <p>Implementations must be immutable: one instance is shared by every player and every group.
 */
public sealed interface TaskReward {

    /** Stable identifier. The screen syncs a pool index, not this, but the log and lookups use it. */
    String id();

    /** What the row's reward strip draws. */
    DailyTaskIcon icon();

    /** Relative likelihood of being drawn against the rest of the pool. Must be positive. */
    int weight();

    /** How the row's tooltip names this reward. */
    Component name();

    void grant(ServerPlayer player);

    /**
     * A stack handed straight over. The icon renders the count, so a stack is all the entry needs
     * to describe itself — no per-reward lang key.
     */
    record Stack(String id, ItemStack stack, int weight) implements TaskReward {

        @Override
        public DailyTaskIcon icon() {
            return DailyTaskIcon.of(stack);
        }

        @Override
        public Component name() {
            return Component.literal(stack.getCount() + " × ").append(stack.getHoverName());
        }

        @Override
        public void grant(ServerPlayer player) {
            // give() copies before handing it over, so the shared table stack is never touched.
            DailyTaskRewards.give(player, stack);
        }
    }

    /**
     * A payout in this mod's own currency, straight through {@link CurrencyService}.
     *
     * <p>The deposit is asynchronous — it goes to the currency database — so the claim returns
     * before the balance lands. A failure is logged by the service rather than surfaced here; the
     * player has already been told the task is claimed either way.
     */
    record Currency(String id, DailyTaskIcon icon, Component name, CurrencyType type, long amount,
                    int weight) implements TaskReward {

        @Override
        public void grant(ServerPlayer player) {
            CurrencyService.deposit(player, type, amount, player.server);
        }
    }

    /**
     * A payout another mod owns, run as a command. Nothing uses this now that the economy is part
     * of this mod, but it stays as the escape hatch for anything the mod cannot pay out itself.
     */
    record Command(String id, DailyTaskIcon icon, Component name, String command, int weight)
            implements TaskReward {

        @Override
        public void grant(ServerPlayer player) {
            DailyTaskRewards.runCommand(player, command);
        }
    }
}
