package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
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

import static com.roll_54.roll_mod.minestar.dailytasks.DailyTasksState.TASK_COUNT;

/** Server-side core of the daily-task system: the 06:00 roll, progress accounting and claiming. */
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

    /** Rolls a new set if the day has turned over (or if the state has never been rolled). */
    public static void rollIfNeeded(MinecraftServer server) {
        DailyTasksState state = state(server);
        long period = currentPeriodDay();
        if (state.periodDay == period && state.taskIds.size() == TASK_COUNT) {
            return;
        }
        roll(server, state, period, new Random(seedFor(server, period)));
    }

    /** Rolls a fresh, deliberately different set right now (the {@code reroll} command). */
    public static void forceRoll(MinecraftServer server) {
        DailyTasksState state = state(server);
        roll(server, state, currentPeriodDay(), new Random());
    }

    private static long seedFor(MinecraftServer server, long period) {
        // Deterministic per world and per day, so a mid-day restart re-derives the same set
        // instead of quietly rerolling it.
        return server.overworld().getSeed() ^ (period * 0x9E3779B97F4A7C15L);
    }

    private static void roll(MinecraftServer server, DailyTasksState state, long period, Random rng) {
        state.periodDay = period;
        state.taskIds.clear();
        state.taskIds.addAll(pick(rng));
        // Clearing every group drops their per-group sets too, so everyone re-seeds from the new
        // default — an admin reroll never outlives the day it was made on.
        state.resetProgress();
        state.setDirty();

        RollMod.LOGGER.info("[DailyTasks] Rolled tasks for period {}: {}", period, state.taskIds);
    }

    /** Draws {@link DailyTasksState#TASK_COUNT} task ids, preferring one per hook. */
    private static List<String> pick(Random rng) {
        List<DailyTask> shuffled = new ArrayList<>(DailyTaskRegistry.all());
        Collections.shuffle(shuffled, rng);

        // Prefer four different hooks so a day never turns into "kill four things".
        List<DailyTask> picked = new ArrayList<>(TASK_COUNT);
        EnumSet<DailyTaskHook> usedHooks = EnumSet.noneOf(DailyTaskHook.class);
        for (DailyTask task : shuffled) {
            if (picked.size() == TASK_COUNT) break;
            if (usedHooks.add(task.hook())) picked.add(task);
        }
        // Only reachable if the registry holds fewer than TASK_COUNT distinct hooks.
        for (DailyTask task : shuffled) {
            if (picked.size() == TASK_COUNT) break;
            if (!picked.contains(task)) picked.add(task);
        }

        return picked.stream().map(DailyTask::id).toList();
    }

    /* ------------------------------------------------- groups ------------------------------------------------- */

    /**
     * This group's state, with its task set seeded from the day's default the first time it is
     * touched. Returns {@code null} before the first roll, or if the group's set is somehow the
     * wrong size — callers treat that as "nothing to show".
     */
    @Nullable
    private static DailyTasksState.GroupState seeded(DailyTasksState state, UUID groupId) {
        if (state.taskIds.size() < TASK_COUNT) return null;

        DailyTasksState.GroupState gs = state.group(groupId);
        if (gs.taskIds.isEmpty()) {
            gs.taskIds.addAll(state.taskIds);
            state.setDirty();
        }
        return gs.taskIds.size() >= TASK_COUNT ? gs : null;
    }

    /**
     * Gives one player's group a fresh set of tasks and wipes its progress, leaving the rest of the
     * server untouched. Note the unit is the <em>group</em>: rerolling someone in an FTB party
     * rerolls the whole party, because they share one set of tasks and one progress record.
     *
     * @return the ids drawn, or {@code null} if no daily set has been rolled yet
     */
    @Nullable
    public static List<String> rerollFor(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        DailyTasksState state = state(server);
        if (state.taskIds.size() < TASK_COUNT) return null;

        DailyTasksState.GroupState gs = state.group(DailyTaskGroups.of(player).id());
        gs.taskIds.clear();
        gs.taskIds.addAll(pick(new Random()));
        gs.clearProgress();
        state.setDirty();

        RollMod.LOGGER.info("[DailyTasks] Rerolled tasks for {}'s group: {}",
                player.getGameProfile().getName(), gs.taskIds);
        return List.copyOf(gs.taskIds);
    }

    /**
     * Wipes one group's progress and claims while keeping its current tasks. Same group caveat as
     * {@link #rerollFor}.
     *
     * @return {@code false} if no daily set has been rolled yet
     */
    public static boolean resetFor(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        DailyTasksState state = state(server);
        DailyTasksState.GroupState gs = seeded(state, DailyTaskGroups.of(player).id());
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
        DailyTasksState.GroupState gs = seeded(state, group.id());
        if (gs == null) return;

        boolean dirty = false;

        for (int i = 0; i < TASK_COUNT; i++) {
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
        if (server == null || index < 0 || index >= TASK_COUNT) return ClaimResult.UNAVAILABLE;

        DailyTasksState state = state(server);
        DailyTasksState.GroupState gs = seeded(state, DailyTaskGroups.of(player).id());
        if (gs == null) return ClaimResult.UNAVAILABLE;

        DailyTask task = DailyTaskRegistry.byId(gs.taskIds.get(index));
        if (task == null) return ClaimResult.UNAVAILABLE;

        if (!gs.completed[index]) return ClaimResult.NOT_COMPLETED;
        if (!gs.claimedBy.get(index).add(player.getUUID())) return ClaimResult.ALREADY_CLAIMED;

        state.setDirty();
        task.grantReward(player);
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

    /**
     * The row at {@code index} for this player, or {@code null} before the first roll. Called both
     * by the GUI's server-side data bindings and by the debug command.
     */
    @Nullable
    public static TaskView view(ServerPlayer player, int index) {
        MinecraftServer server = player.getServer();
        if (server == null || index < 0 || index >= TASK_COUNT) return null;

        DailyTasksState state = state(server);
        DailyTaskGroups.TaskGroup group = DailyTaskGroups.of(player);
        DailyTasksState.GroupState gs = seeded(state, group.id());
        if (gs == null) return null;

        DailyTask task = DailyTaskRegistry.byId(gs.taskIds.get(index));
        if (task == null) return null;

        return new TaskView(task, gs.progress[index], requiredAmount(task, group),
                gs.completed[index], gs.claimedBy.get(index).contains(player.getUUID()));
    }

    /** Debug helper behind {@code /rollmod dailytasks progress}. */
    public static void addRawProgress(ServerPlayer player, int index, int amount) {
        MinecraftServer server = player.getServer();
        if (server == null || index < 0 || index >= TASK_COUNT) return;

        DailyTasksState state = state(server);
        DailyTaskGroups.TaskGroup group = DailyTaskGroups.of(player);
        DailyTasksState.GroupState gs = seeded(state, group.id());
        if (gs == null) return;

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
