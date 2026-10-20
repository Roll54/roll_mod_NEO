package com.roll_54.roll_mod.minestar.leaderboard;

import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * The leaderboard tab's drop-down. Every board is seasonal: it reads this season's data — the
 * balances and money history the admin wipe resets, this world's quest file, statistics and daily
 * task counts. New boards go last: the packets send the ordinal.
 */
public enum LeaderboardCategory {
    /** Main money balance. */
    RICHEST,
    /** Required FTB quests completed, per team: quest progress is shared by a party. */
    QUESTS,
    /** Time played on this server, from the vanilla statistics (ticks). */
    PLAYTIME,
    /** Income from selling to other players over the last 7 days, net of the auction fee. */
    EARNERS_7D,
    /** Daily task rewards claimed, per player, over this world's lifetime. */
    DAILY_TASKS;

    public Component title() {
        return Component.translatable("gui.roll_mod.hub.leaderboards.category." + name().toLowerCase(Locale.ROOT));
    }

    /** Whether rows are teams (no face to draw) rather than players. */
    public boolean teams() {
        return this == QUESTS;
    }

    public Component format(long value) {
        return switch (this) {
            case RICHEST -> Component.literal(money(value));
            case EARNERS_7D -> Component.literal("+" + money(value));
            case QUESTS -> Component.translatable("gui.roll_mod.hub.leaderboards.quests", value);
            case DAILY_TASKS -> Component.translatable("gui.roll_mod.hub.leaderboards.daily_tasks", value);
            case PLAYTIME -> {
                long minutes = value / 20 / 60;
                yield Component.translatable("gui.roll_mod.hub.leaderboards.playtime", minutes / 60, minutes % 60);
            }
        };
    }

    private static String money(long amount) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(amount).replace(',', ' ');
    }
}
