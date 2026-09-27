package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.minestar.moderation.ModerationStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps each player's "daily rewards to collect" count current on their client, where the hub
 * button and the Daily Tasks bookmark read it to show the attention badge.
 *
 * <p>A dirty flag rather than a push per call site: completion fans out across a whole party, a
 * day's turnover touches everybody, and an admin reset can hit anyone — so any of those just marks
 * the count stale, and {@link #tick} refreshes everyone online at most once a second. The count
 * rides the always-on player status packet ({@code ModerationStatus}), because the daily-task
 * screen's own data only exists while the hub is open.
 */
public final class DailyTaskStatus {

    private static volatile boolean dirty;

    private DailyTaskStatus() {}

    public static void markDirty() {
        dirty = true;
    }

    /** Called every server tick; does work at most once a second, and only when marked. */
    public static void tick(MinecraftServer server) {
        if (!dirty || server.getTickCount() % 20 != 0) return;
        dirty = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModerationStatus.sendTo(player);
        }
    }
}
