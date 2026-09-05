package com.roll_54.roll_mod.minestar.dailytasks.api;

import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskGroups;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTasksState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * How many daily tasks a player is dealt.
 *
 * <p>Every player gets {@link #DEFAULT} today. This class exists so that a later feature — a rank,
 * a permission, a progression unlock — can hand out different numbers without the count being a
 * constant threaded through the save format, the roll and the screen: swap the provider with
 * {@link #setProvider} and everything downstream follows.
 *
 * <p>A quota only takes effect at the group's <em>next daily roll</em>. Applying it mid-period
 * would mean redrawing a set the party has already made progress on, and progress is shared, so
 * one player's new quota would wipe everyone's day.
 *
 * <p>{@link DailyTasksState#MAX_TASK_COUNT} is the hard ceiling — it sizes the saved arrays and the
 * screen's rows, so no provider can exceed it.
 */
public final class DailyTaskQuota {

    /** What a player is dealt with no provider installed. */
    public static final int DEFAULT = 4;

    private static volatile ToIntFunction<ServerPlayer> provider = player -> DEFAULT;

    private DailyTaskQuota() {}

    /**
     * Installs the rule that decides a player's quota. Called on the server; the provider runs on
     * the server thread during a daily roll, so it must not block.
     */
    public static void setProvider(ToIntFunction<ServerPlayer> provider) {
        DailyTaskQuota.provider = provider == null ? player -> DEFAULT : provider;
    }

    /** One player's quota, clamped to a drawable range. */
    public static int forPlayer(ServerPlayer player) {
        return clamp(provider.applyAsInt(player));
    }

    /**
     * A group draws one set between them, so it needs a single number: the largest quota among the
     * members who are online to be asked. Nobody online — which happens when a group is rolled from
     * a scheduled tick rather than a login — falls back to {@link #DEFAULT}.
     */
    public static int forGroup(MinecraftServer server, DailyTaskGroups.TaskGroup group) {
        int quota = 0;
        for (UUID member : group.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) quota = Math.max(quota, forPlayer(player));
        }
        return quota == 0 ? DEFAULT : quota;
    }

    private static int clamp(int quota) {
        return Math.max(1, Math.min(DailyTasksState.MAX_TASK_COUNT, quota));
    }
}
