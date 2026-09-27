package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskQuota;
import com.roll_54.roll_mod.minestar.dailytasks.api.TaskReward;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
        state.freeRerollsUsed.clear();
        state.setDirty();
        DailyTaskStatus.markDirty();
        RollMod.LOGGER.info("[DailyTasks] Daily period is now {}; stale groups pruned.", period);
    }

    /**
     * Wipes every group, so each one draws a brand new set (and a new bonus reward) the next time
     * it is touched. Backs the no-argument {@code reroll} command.
     *
     * <p>Clearing alone is not enough: the draw is seeded by world, group and day, so a cleared
     * group would draw exactly what it just lost. Bumping {@link DailyTasksState#rerollSalt} moves
     * the seed.
     */
    public static void forceRoll(MinecraftServer server) {
        DailyTasksState state = state(server);
        state.rerollSalt++;
        state.groups.clear();
        state.periodDay = currentPeriodDay();
        state.setDirty();
        DailyTaskStatus.markDirty();
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
        // Row rewards (rewardsFor) deliberately leave the salt out, so a reroll keeps what each row pays.
        Random rng = new Random(seedFor(server, group.id(), period) ^ (state.rerollSalt * REROLL_SALT));
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
        Arrays.fill(gs.rerolls, 0);
        state.setDirty();
        DailyTaskStatus.markDirty();

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

    /**
     * Separates the reward draw from the task draw, which share {@link #seedFor}'s seed. Any odd
     * constant does; this one is a 64-bit prime-ish mix so neighbouring row indices land far apart.
     */
    private static final long REWARD_SALT = 0x8AA1F0F1B3D9C7E5L;

    /** Spreads {@link DailyTasksState#rerollSalt} across the seed, so reroll N and N+1 draw unrelated sets. */
    private static final long REROLL_SALT = 0x94D049BB133111EBL;

    /**
     * What the row at {@code index} pays, on top of {@link TaskRewardPool#STARCOINS}.
     *
     * <p>Derived rather than stored: the seed is the day's, the group's and the row's, so every
     * read — the screen, the tooltip, the payout — agrees, and a restart re-derives the same draw.
     * Keying on the row index rather than the task means a {@code reroll} changes the tasks while
     * each slot keeps the rewards it was advertising.
     *
     * <p>The one thing that does move it is editing {@link TaskRewardPool} itself, which restates a
     * board mid-day. That self-corrects at the next 06:00 roll.
     */
    public static List<TaskReward> rewardsFor(MinecraftServer server,
                                              DailyTaskGroups.TaskGroup group, int index) {
        Random rng = new Random(seedFor(server, group.id(), currentPeriodDay())
                + index * REWARD_SALT);
        return TaskRewardPool.draw(rng, TaskRewardPool.NUM_PER_TASK);
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
        Arrays.fill(gs.rerolls, 0);
        state.setDirty();
        DailyTaskStatus.markDirty();

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
        DailyTaskStatus.markDirty();

        RollMod.LOGGER.info("[DailyTasks] Cleared daily-task progress for {}'s group.",
                player.getGameProfile().getName());
        return true;
    }

    /* ------------------------------------------------ rerolling ----------------------------------------------- */

    /**
     * What a slot's 1st, 2nd and 3rd reroll of the day cost in Starcoins. Its length is also the
     * cap: a slot rerolled this many times keeps its task until the next 06:00 roll.
     */
    private static final long[] REROLL_COSTS = {100, 150, 200};

    /** Players with a paid reroll still waiting on the currency database — a second click is dropped. */
    private static final Set<UUID> PENDING_REROLLS = new HashSet<>();

    /**
     * What rerolling slot {@code index} would cost this player right now: {@code 0} when it would be
     * free (their {@code rollmod.dailytasks.freererolls} allowance is not used up), the slot's next
     * price otherwise, and {@code -1} when it cannot be rerolled at all — no such slot, the task is
     * already done, or the slot has used every reroll it gets today. The screen hides its button on
     * {@code -1}.
     */
    public static long rerollCost(ServerPlayer player, int index) {
        MinecraftServer server = player.getServer();
        if (server == null || index < 0 || index >= MAX_TASK_COUNT) return -1;

        DailyTasksState state = state(server);
        DailyTasksState.GroupState gs = current(server, state, DailyTaskGroups.of(player));
        if (gs == null || index >= gs.taskCount() || gs.completed[index]) return -1;
        if (gs.rerolls[index] >= REROLL_COSTS.length) return -1;

        return freeRerollsLeft(state, player) > 0 ? 0 : REROLL_COSTS[gs.rerolls[index]];
    }

    /** How many rerolls this player may still make free of charge today. */
    public static int freeRerollsLeft(DailyTasksState state, ServerPlayer player) {
        int used = state.freeRerollsUsed.getOrDefault(player.getUUID(), 0);
        return Math.max(0, LuckPermsCompat.dailyTaskFreeRerolls(player) - used);
    }

    /** How many rerolls slot {@code index} has left today, for the button's tooltip. */
    public static int rerollsLeft(ServerPlayer player, int index) {
        MinecraftServer server = player.getServer();
        if (server == null || index < 0 || index >= MAX_TASK_COUNT) return 0;
        DailyTasksState.GroupState gs = current(server, state(server), DailyTaskGroups.of(player));
        if (gs == null) return 0;
        return Math.max(0, REROLL_COSTS.length - gs.rerolls[index]);
    }

    /**
     * Swaps the task in slot {@code index} for a different one, charging the clicking player. Any
     * party member may do it — the slot is the party's, the bill and the free allowance are the
     * clicker's own. The new task starts from zero; the slot keeps the rewards it was advertising,
     * because those are keyed on the slot rather than the task (see {@link #rewardsFor}).
     *
     * <p>A paid reroll withdraws first and applies once the database answers, back on the server
     * thread. Anything that moved the slot meanwhile — another member rerolling it, the task being
     * finished, the day turning over — refunds the charge instead of rerolling a slot the player
     * never saw.
     */
    public static void rerollSlot(ServerPlayer player, int index) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        long cost = rerollCost(player, index);
        if (cost < 0) return;

        DailyTasksState state = state(server);
        DailyTasksState.GroupState gs = current(server, state, DailyTaskGroups.of(player));
        if (gs == null) return;
        String taskId = gs.taskIds.get(index);
        int rerolls = gs.rerolls[index];

        if (cost == 0) {
            if (applyReroll(player, state, gs, index)) {
                state.freeRerollsUsed.merge(player.getUUID(), 1, Integer::sum);
                state.setDirty();
            }
            return;
        }

        if (!PENDING_REROLLS.add(player.getUUID())) return;
        String who = player.getGameProfile().getName();
        CurrencyService.withdraw(player, CurrencyType.MAIN, cost)
                .thenAcceptAsync(ok -> {
                    PENDING_REROLLS.remove(player.getUUID());
                    if (!ok) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.roll_mod.daily_tasks.reroll.cannotAfford", cost)
                                .withStyle(ChatFormatting.RED));
                        return;
                    }
                    DailyTasksState.GroupState now = current(server, state(server), DailyTaskGroups.of(player));
                    boolean unchanged = now == gs && index < now.taskCount()
                            && now.taskIds.get(index).equals(taskId)
                            && now.rerolls[index] == rerolls && !now.completed[index];
                    if (!unchanged || !applyReroll(player, state(server), now, index)) {
                        CurrencyService.deposit(player, CurrencyType.MAIN, cost, server);
                        RollMod.LOGGER.info("[DailyTasks] Refunded {} {} for a reroll of slot {} that moved.",
                                who, cost, index);
                    }
                }, server)
                .exceptionally(e -> {
                    server.execute(() -> PENDING_REROLLS.remove(player.getUUID()));
                    RollMod.LOGGER.error("[DailyTasks] Paid reroll of slot {} for {} failed; refunding {}.",
                            index, who, cost, e);
                    CurrencyService.deposit(player, CurrencyType.MAIN, cost, server);
                    return null;
                });
    }

    /**
     * Rerolls slot {@code index} for free, outside the daily cap — the admin command's path.
     *
     * @return the new task id, or {@code null} if the slot does not exist or nothing else is left
     */
    @Nullable
    public static String forceRerollSlot(ServerPlayer player, int index) {
        MinecraftServer server = player.getServer();
        if (server == null || index < 0 || index >= MAX_TASK_COUNT) return null;
        DailyTasksState state = state(server);
        DailyTasksState.GroupState gs = current(server, state, DailyTaskGroups.of(player));
        if (gs == null || index >= gs.taskCount()) return null;

        String replacement = pickReplacement(gs, index, new Random());
        if (replacement == null) return null;
        replaceSlot(gs, index, replacement);
        state.setDirty();
        DailyTaskStatus.markDirty();
        return replacement;
    }

    /** Draws the replacement, spends one of the slot's rerolls and tells the player what they got. */
    private static boolean applyReroll(ServerPlayer player, DailyTasksState state,
                                       DailyTasksState.GroupState gs, int index) {
        String replacement = pickReplacement(gs, index, new Random());
        if (replacement == null) return false;

        String old = gs.taskIds.get(index);
        replaceSlot(gs, index, replacement);
        gs.rerolls[index]++;
        state.setDirty();
        DailyTaskStatus.markDirty();

        DailyTask task = DailyTaskRegistry.byId(replacement);
        if (task != null) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.daily_tasks.reroll.done",
                    task.name()).withStyle(ChatFormatting.AQUA));
        }
        RollMod.LOGGER.info("[DailyTasks] {} rerolled slot {}: {} -> {} ({} of {}).",
                player.getGameProfile().getName(), index, old, replacement,
                gs.rerolls[index], REROLL_COSTS.length);
        return true;
    }

    /** Puts {@code taskId} into the slot and starts it from nothing. */
    private static void replaceSlot(DailyTasksState.GroupState gs, int index, String taskId) {
        gs.taskIds.set(index, taskId);
        gs.progress[index] = 0;
        gs.completed[index] = false;
        gs.claimedBy.get(index).clear();
        gs.invalidateHooks();
    }

    /**
     * One task for slot {@code index}: never one already on the board, the slot's own included,
     * and — as {@link #pick} does for a whole set — preferably one whose hook no other slot uses.
     *
     * @return {@code null} if every registered task is already on the board
     */
    @Nullable
    private static String pickReplacement(DailyTasksState.GroupState gs, int index, Random rng) {
        List<DailyTask> shuffled = new ArrayList<>(DailyTaskRegistry.all());
        Collections.shuffle(shuffled, rng);

        EnumSet<DailyTaskHook> otherHooks = EnumSet.noneOf(DailyTaskHook.class);
        for (int i = 0; i < gs.taskCount(); i++) {
            if (i == index) continue;
            DailyTask other = DailyTaskRegistry.byId(gs.taskIds.get(i));
            if (other != null) otherHooks.add(other.hook());
        }

        DailyTask fallback = null;
        for (DailyTask task : shuffled) {
            if (gs.taskIds.contains(task.id())) continue;
            if (!otherHooks.contains(task.hook())) return task.id();
            if (fallback == null) fallback = task;
        }
        return fallback == null ? null : fallback.id();
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

        // Fast miss path: this runs on every block break, kill, craft etc. server-wide, so when
        // the board has no open task for the hook, get out before the full group resolve below
        // (which copies the party's member set) and the slot loop. A group that is missing or
        // stale-day falls through, so the lazy daily roll still happens on its first event.
        DailyTasksState.GroupState fast = state.groups.get(DailyTaskGroups.idOf(player));
        if (fast != null && fast.periodDay == currentPeriodDay()
                && !fast.activeHooks().contains(hook)) {
            return;
        }

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
                gs.invalidateHooks();
                DailyTaskNotifier.announce(server, group, task, required, player);
            }
        }

        if (dirty) state.setDirty();
    }

    /**
     * The scaled requirement. A player on their own — including the only member of their own party
     * — is asked for {@code base} exactly; from two members up the bar is {@code base * (members +
     * 1)}, so a pair sees {@code base * 3} and a party of three {@code base * 4}. A task that
     * declines to scale (see {@link DailyTask#scalesWithTeam()}) always asks for {@code base}, and
     * one with its own {@link DailyTask#teamMultiplier()} compounds instead: {@code base *
     * m^(members - 1)}, rounded up.
     *
     * <p>Computed on read rather than frozen at roll time, so it tracks a party that grows or
     * shrinks during the day. Completion is sticky (see {@link DailyTasksState.GroupState}), so a
     * task that is already done never reverts when the bar moves up — and a bar that moves down,
     * as it does when a party breaks up, simply completes a task whose progress is already past it.
     */
    public static int requiredAmount(DailyTask task, DailyTaskGroups.TaskGroup group) {
        int members = group.memberCount();
        if (!task.scalesWithTeam() || members <= 1) return task.baseAmount();
        double m = task.teamMultiplier();
        // The epsilon keeps an exact product like 16 * 1.5 from ceiling up past itself.
        if (m > 0) return (int) Math.ceil(task.baseAmount() * Math.pow(m, members - 1) - 1e-9);
        return task.baseAmount() * (members + 1);
    }

    /* ------------------------------------------------- claiming ----------------------------------------------- */

    public enum ClaimResult { OK, NOT_COMPLETED, ALREADY_CLAIMED, UNAVAILABLE }

    public static ClaimResult claim(ServerPlayer player, int index) {
        MinecraftServer server = player.getServer();
        if (server == null || index < 0 || index >= MAX_TASK_COUNT) return ClaimResult.UNAVAILABLE;

        DailyTasksState state = state(server);
        DailyTaskGroups.TaskGroup group = DailyTaskGroups.of(player);
        DailyTasksState.GroupState gs = current(server, state, group);
        // A group holding fewer tasks than the cap leaves the trailing slots empty.
        if (gs == null || index >= gs.taskCount()) return ClaimResult.UNAVAILABLE;

        DailyTask task = DailyTaskRegistry.byId(gs.taskIds.get(index));
        if (task == null) return ClaimResult.UNAVAILABLE;

        if (!gs.completed[index]) return ClaimResult.NOT_COMPLETED;
        if (!gs.claimedBy.get(index).add(player.getUUID())) return ClaimResult.ALREADY_CLAIMED;

        state.setDirty();
        DailyTaskStatus.markDirty();
        // The same draw the row has been advertising all day, plus the flat currency payout. Paid
        // per player, not per group: progress is shared, the reward is not.
        for (TaskReward reward : rewardsFor(server, group, index)) {
            reward.grant(player);
        }
        TaskRewardPool.STARCOINS.grant(player);
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
        DailyTaskStatus.markDirty();
        reward.grant(player);
        RollMod.LOGGER.info("[DailyTasks] {} collected the daily bonus '{}'.",
                player.getGameProfile().getName(), reward.id());
        return ClaimResult.OK;
    }

    /**
     * How many rewards this player could collect right now: finished tasks they have not claimed,
     * plus one for the bonus once the whole board is done and they have not taken it. Mirrors the
     * checks {@link #claim} and {@link #claimBonus} make, so a badge never promises a claim they
     * would refuse.
     */
    public static int claimableCount(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return 0;
        DailyTasksState.GroupState gs = current(server, state(server), DailyTaskGroups.of(player));
        if (gs == null) return 0;

        int count = 0;
        for (int i = 0; i < gs.taskCount(); i++) {
            if (gs.completed[i] && !gs.claimedBy.get(i).contains(player.getUUID())
                    && DailyTaskRegistry.byId(gs.taskIds.get(i)) != null) {
                count++;
            }
        }
        if (gs.completedCount() >= gs.taskCount() && gs.taskCount() > 0
                && DailyRewardRegistry.byId(gs.rewardId) != null
                && !gs.rewardClaimedBy.contains(player.getUUID())) {
            count++;
        }
        return count;
    }

    /* -------------------------------------------------- views ------------------------------------------------- */

    public static DailyTasksState state(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return DailyTasksState.get(overworld);
    }

    /**
     * A snapshot of one task row as it should appear to {@code player}.
     *
     * @param rewards what claiming it pays, less the flat {@link TaskRewardPool#STARCOINS} every
     *                task adds — the screen appends that itself rather than syncing a constant
     */
    public record TaskView(DailyTask task, int progress, int required, boolean completed,
                           boolean claimed, List<TaskReward> rewards) {}

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
                gs.completed[index], gs.claimedBy.get(index).contains(player.getUUID()),
                rewardsFor(server, group, index));
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

    /** Debug helper behind {@code /rollmod admin dailytasks progress}. */
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
            gs.invalidateHooks();
            DailyTaskNotifier.announce(server, group, task, required, player);
        }
        state.setDirty();
    }
}
