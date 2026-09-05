package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskQuota;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.roll_54.roll_mod.minestar.dailytasks.DailyTasksState.MAX_TASK_COUNT;

/** Server-side core of the daily-task system: the per-group roll, progress accounting and claiming. */
public final class DailyTaskManager {

    /** Tasks roll over at 06:00 in the server machine's own time zone. */
    public static final int RESET_HOUR = 6;

    private DailyTaskManager() {}

    /* ------------------------------------------------- clock ------------------------------------------------- */

    private static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    /**
     * The epoch day of the current task period. The period starting at 06:00 today is labelled with
     * today's date; anything before 06:00 still belongs to yesterday's period.
     *
     * <p>Comparing the local wall-clock time against 06:00 (rather than subtracting six hours from
     * an instant) keeps the boundary exact across DST changes.
     */
    public static long currentPeriodDay() {
        LocalDateTime now = LocalDateTime.now(zone());
        LocalDate day = now.toLocalTime().isBefore(LocalTime.of(RESET_HOUR, 0))
                ? now.toLocalDate().minusDays(1)
                : now.toLocalDate();
        return day.toEpochDay();
    }

    /** Seconds until the next 06:00, for the "resets in" readout in the GUI. */
    public static long secondsUntilNextRoll() {
        ZonedDateTime now = ZonedDateTime.now(zone());
        ZonedDateTime next = now.toLocalDate().atTime(RESET_HOUR, 0).atZone(now.getZone());
        if (!next.isAfter(now)) {
            next = next.plusDays(1); // ZonedDateTime.plusDays keeps the wall-clock hour across DST
        }
        return Math.max(0, Duration.between(now, next).toSeconds());
    }

    /* -------------------------------------------------- roll -------------------------------------------------- */

    /**
     * Day-turnover bookkeeping. Assigns nothing — each group draws its own set the first time it is
     * touched on a new day, which for a player is normally their first login (see
     * {@code DailyTaskEvents.onLoggedIn}). All this does is notice the boundary and drop the
     * previous day's groups, which nothing else clears any more.
     */
    public static void rollIfNeeded(MinecraftServer server) {
        DailyTasksState state = state(server);
        long period = currentPeriodDay();
        if (state.periodDay == period) return;

        state.periodDay = period;
        state.pruneStaleGroups(period);
        state.setDirty();
        RollMod.LOGGER.info("[DailyTasks] Daily period is now {}; stale groups pruned.", period);
    }

    /**
     * Wipes every group, so each one draws a brand new set (and a new bonus reward) the next time
     * it is touched. Backs the no-argument {@code reroll} command.
     */
    public static void forceRoll(MinecraftServer server) {
        DailyTasksState state = state(server);
        state.groups.clear();
        state.periodDay = currentPeriodDay();
        state.setDirty();
        RollMod.LOGGER.info("[DailyTasks] Cleared every group; all will reroll on next touch.");
    }

    /**
     * Draws this group's tasks and bonus reward if it has not been rolled for {@code period} yet,
     * and wipes whatever the previous day left behind.
     *
     * <p>The seed mixes the world, the group and the day, so a mid-day restart re-derives exactly
     * the same set for each group rather than quietly rerolling it.
     *
     * @return the group's state, or {@code null} if no task could be drawn at all
     */
    @Nullable
    private static DailyTasksState.GroupState rollGroupIfNeeded(
            MinecraftServer server, DailyTasksState state,
            DailyTaskGroups.TaskGroup group, long period) {

        DailyTasksState.GroupState gs = state.group(group.id());
        // A set already drawn for today stands even if a member's quota has since changed:
        // redrawing mid-period would wipe progress the whole party shares.
        if (gs.periodDay == period && !gs.taskIds.isEmpty()) {
            return gs;
        }

        int quota = DailyTaskQuota.forGroup(server, group);
        Random rng = new Random(seedFor(server, group.id(), period));
        List<String> picked = pick(rng, quota);
        if (picked.size() < quota) {
            // Fewer tasks are registered than the group is owed; there is nothing to show.
            return null;
        }

        gs.periodDay = period;
        gs.taskIds.clear();
        gs.taskIds.addAll(picked);
        gs.rewardId = pickReward(rng);
        gs.clearProgress();
        state.setDirty();

        RollMod.LOGGER.info("[DailyTasks] Rolled group {} for period {}: {} (bonus: {})",
                group.id(), period, gs.taskIds, gs.rewardId.isEmpty() ? "<none>" : gs.rewardId);
        return gs;
    }

    /** Public entry point for the login hook: make sure this player's group is on today's set. */
    public static void rollGroupIfNeeded(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        rollGroupIfNeeded(server, state(server), DailyTaskGroups.of(player), currentPeriodDay());
    }

    private static long seedFor(MinecraftServer server, UUID groupId, long period) {
        return server.overworld().getSeed()
                ^ (groupId.getMostSignificantBits() * 0x9E3779B97F4A7C15L)
                ^ (groupId.getLeastSignificantBits() * 0xC2B2AE3D27D4EB4FL)
                ^ (period * 0xBF58476D1CE4E5B9L);
    }

    /** Draws {@code count} task ids, preferring one per hook. */
    private static List<String> pick(Random rng, int count) {
        List<DailyTask> shuffled = new ArrayList<>(DailyTaskRegistry.all());
        Collections.shuffle(shuffled, rng);

        // Prefer a different hook per task so a day never turns into "kill four things".
        List<DailyTask> picked = new ArrayList<>(count);
        EnumSet<DailyTaskHook> usedHooks = EnumSet.noneOf(DailyTaskHook.class);
        for (DailyTask task : shuffled) {
            if (picked.size() == count) break;
            if (usedHooks.add(task.hook())) picked.add(task);
        }
        // Only reachable if the registry holds fewer than {@code count} distinct hooks.
        for (DailyTask task : shuffled) {
            if (picked.size() == count) break;
            if (!picked.contains(task)) picked.add(task);
        }

        return picked.stream().map(DailyTask::id).toList();
    }

    /**
     * Draws the day's bonus reward, honouring {@link DailyReward#weight()}. Returns an empty string
     * when no reward is registered, which the GUI shows as an empty panel.
     */
    private static String pickReward(Random rng) {
        List<DailyReward> rewards = DailyRewardRegistry.all();
        if (rewards.isEmpty()) return "";

        int total = 0;
        for (DailyReward reward : rewards) {
            total += reward.weight(); // the registry has already rejected non-positive weights
        }

        int roll = rng.nextInt(total);
        for (DailyReward reward : rewards) {
            roll -= reward.weight();
            if (roll < 0) return reward.id();
        }
        return rewards.getLast().id(); // unreachable; the loop above always lands
    }

    /* ------------------------------------------------- groups ------------------------------------------------- */

    /**
     * This group's state for <em>today</em>, rolling it first if the day has turned over. Returns
     * {@code null} only when nothing can be drawn — too few tasks registered.
     *
     * <p>Every entry point below goes through this, which is what actually guarantees a player
     * never acts on a stale day: the login hook only makes the roll happen promptly.
     */
    @Nullable
    private static DailyTasksState.GroupState current(MinecraftServer server, DailyTasksState state,
                                                      DailyTaskGroups.TaskGroup group) {
        return rollGroupIfNeeded(server, state, group, currentPeriodDay());
    }

    /**
     * Gives one player's group a fresh set of tasks and a fresh bonus reward, wiping its progress
     * and leaving the rest of the server untouched. Note the unit is the <em>group</em>: rerolling
     * someone in an FTB party rerolls the whole party, because they share one set of tasks and one
     * progress record.
     *
     * @return the ids drawn, or {@code null} if nothing could be drawn
     */
    @Nullable
    public static List<String> rerollFor(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        DailyTasksState state = state(server);
        Random rng = new Random();
        int quota = DailyTaskQuota.forPlayer(player);
        List<String> picked = pick(rng, quota);
        if (picked.size() < quota) return null;

        DailyTasksState.GroupState gs = state.group(DailyTaskGroups.of(player).id());
        gs.periodDay = currentPeriodDay();
        gs.taskIds.clear();
        gs.taskIds.addAll(picked);
        gs.rewardId = pickReward(rng);
        gs.clearProgress();
        state.setDirty();

        RollMod.LOGGER.info("[DailyTasks] Rerolled tasks for {}'s group: {} (bonus: {})",
                player.getGameProfile().getName(), gs.taskIds,
                gs.rewardId.isEmpty() ? "<none>" : gs.rewardId);
        return List.copyOf(gs.taskIds);
    }

    /**
     * Wipes one group's progress and claims while keeping its current tasks and reward. Same group
     * caveat as {@link #rerollFor}.
     *
     * @return {@code false} if nothing has been drawn for this group
     */
    public static boolean resetFor(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        DailyTasksState state = state(server);
        DailyTasksState.GroupState gs = current(server, state, DailyTaskGroups.of(player));
        if (gs == null) return false;

        gs.clearProgress();
        state.setDirty();

        RollMod.LOGGER.info("[DailyTasks] Cleared daily-task progress for {}'s group.",
                player.getGameProfile().getName());
        return true;
    }

    /* ------------------------------------------------ progress ------------------------------------------------ */

    /**
     * The single funnel every event hook calls.
     *
     * @param subject a BlockState, ItemStack or LivingEntity, matching {@code hook} — see
     *                {@link DailyTaskHook}
     */
    public static void progress(ServerPlayer player, DailyTaskHook hook, Object subject, int amount) {
        if (amount <= 0 || player.isSpectator()) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        DailyTasksState state = state(server);
        DailyTaskGroups.TaskGroup group = DailyTaskGroups.of(player);
        DailyTasksState.GroupState gs = current(server, state, group);
        if (gs == null) return;

        boolean dirty = false;

        for (int i = 0; i < gs.taskCount(); i++) {
            DailyTask task = DailyTaskRegistry.byId(gs.taskIds.get(i));
            if (task == null || task.hook() != hook || gs.completed[i] || !task.matches(subject)) {
                continue;
            }

            int required = requiredAmount(task, group);
            gs.progress[i] = Math.min(required, gs.progress[i] + amount);
            dirty = true;

            if (gs.progress[i] >= required) {
                gs.completed[i] = true;
                DailyTaskNotifier.announce(server, group, task, required, player);
            }
        }

        if (dirty) state.setDirty();
    }

    /**
     * The scaled requirement: {@code base * (members + 1)}. A player in no party counts as one
     * member, so they see {@code base * 2}; a party of three sees {@code base * 4}.
     *
     * <p>Computed on read rather than frozen at roll time, so it tracks a party that grows or
     * shrinks during the day. Completion is sticky (see {@link DailyTasksState.GroupState}), so a
     * task that is already done never reverts when the bar moves up.
     */
    public static int requiredAmount(DailyTask task, DailyTaskGroups.TaskGroup group) {
        return task.baseAmount() * (group.memberCount() + 1);
    }

    /* ------------------------------------------------- claiming ----------------------------------------------- */

    public enum ClaimResult { OK, NOT_COMPLETED, ALREADY_CLAIMED, UNAVAILABLE }

    public static ClaimResult claim(ServerPlayer player, int index) {
        MinecraftServer server = player.getServer();
        if (server == null || index < 0 || index >= MAX_TASK_COUNT) return ClaimResult.UNAVAILABLE;

        DailyTasksState state = state(server);
        DailyTasksState.GroupState gs = current(server, state, DailyTaskGroups.of(player));
        // A group holding fewer tasks than the cap leaves the trailing slots empty.
        if (gs == null || index >= gs.taskCount()) return ClaimResult.UNAVAILABLE;

        DailyTask task = DailyTaskRegistry.byId(gs.taskIds.get(index));
        if (task == null) return ClaimResult.UNAVAILABLE;

        if (!gs.completed[index]) return ClaimResult.NOT_COMPLETED;
        if (!gs.claimedBy.get(index).add(player.getUUID())) return ClaimResult.ALREADY_CLAIMED;

        state.setDirty();
        task.grantReward(player);
        return ClaimResult.OK;
    }

    /**
     * Takes the all-complete bonus. Re-validates everything, so a spoofed packet from a player who
     * has not finished the board — or who has already collected — is simply rejected.
     */
    public static ClaimResult claimBonus(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return ClaimResult.UNAVAILABLE;

        DailyTasksState state = state(server);
        DailyTasksState.GroupState gs = current(server, state, DailyTaskGroups.of(player));
        if (gs == null) return ClaimResult.UNAVAILABLE;

        DailyReward reward = DailyRewardRegistry.byId(gs.rewardId);
        if (reward == null) return ClaimResult.UNAVAILABLE;

        if (gs.completedCount() < gs.taskCount()) return ClaimResult.NOT_COMPLETED;
        if (!gs.rewardClaimedBy.add(player.getUUID())) return ClaimResult.ALREADY_CLAIMED;

        state.setDirty();
        reward.grant(player);
        RollMod.LOGGER.info("[DailyTasks] {} collected the daily bonus '{}'.",
                player.getGameProfile().getName(), reward.id());
        return ClaimResult.OK;
    }

    /* -------------------------------------------------- views ------------------------------------------------- */

    public static DailyTasksState state(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return DailyTasksState.get(overworld);
    }

    /** A snapshot of one task row as it should appear to {@code player}. */
    public record TaskView(DailyTask task, int progress, int required, boolean completed,
                           boolean claimed) {}

    /** A snapshot of the all-complete bonus panel as it should appear to {@code player}. */
    public record BonusView(DailyReward reward, int completed, int total, boolean claimed) {}

    /**
     * The row at {@code index} for this player, or {@code null} when nothing has been drawn. Called
     * both by the GUI's server-side data bindings and by the debug command.
     */
    @Nullable
    public static TaskView view(ServerPlayer player, int index) {
        MinecraftServer server = player.getServer();
        if (server == null || index < 0 || index >= MAX_TASK_COUNT) return null;

        DailyTasksState state = state(server);
        DailyTaskGroups.TaskGroup group = DailyTaskGroups.of(player);
        DailyTasksState.GroupState gs = current(server, state, group);
        // Null for a slot past this group's quota — the screen hides those rows.
        if (gs == null || index >= gs.taskCount()) return null;

        DailyTask task = DailyTaskRegistry.byId(gs.taskIds.get(index));
        if (task == null) return null;

        return new TaskView(task, gs.progress[index], requiredAmount(task, group),
                gs.completed[index], gs.claimedBy.get(index).contains(player.getUUID()));
    }

    /** The bonus panel for this player, or {@code null} when no reward is drawn or registered. */
    @Nullable
    public static BonusView bonusView(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        DailyTasksState state = state(server);
        DailyTasksState.GroupState gs = current(server, state, DailyTaskGroups.of(player));
        if (gs == null) return null;

        DailyReward reward = DailyRewardRegistry.byId(gs.rewardId);
        if (reward == null) return null;

        return new BonusView(reward, gs.completedCount(), gs.taskCount(),
                gs.rewardClaimedBy.contains(player.getUUID()));
    }

    /** Debug helper behind {@code /rollmod dailytasks progress}. */
    public static void addRawProgress(ServerPlayer player, int index, int amount) {
        MinecraftServer server = player.getServer();
        if (server == null || index < 0 || index >= MAX_TASK_COUNT) return;

        DailyTasksState state = state(server);
        DailyTaskGroups.TaskGroup group = DailyTaskGroups.of(player);
        DailyTasksState.GroupState gs = current(server, state, group);
        if (gs == null || index >= gs.taskCount()) return;

        DailyTask task = DailyTaskRegistry.byId(gs.taskIds.get(index));
        if (task == null) return;

        if (gs.completed[index]) return;

        int required = requiredAmount(task, group);
        gs.progress[index] = Math.min(required, gs.progress[index] + amount);
        if (gs.progress[index] >= required) {
            gs.completed[index] = true;
            DailyTaskNotifier.announce(server, group, task, required, player);
        }
        state.setDirty();
    }
}
