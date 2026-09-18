package com.roll_54.roll_mod.minestar.dailytasks;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The whole daily-task system's persistent state, kept on the overworld's data storage (same place
 * and style as {@code netherstorm/StormState}).
 *
 * <p>Everything is keyed by <em>group</em>, not by player: a group is an FTB party (keyed by its
 * team id) or, for players in no party, the player themself (keyed by their own UUID). See
 * {@link DailyTaskGroups}.
 *
 * <p>Each group owns its task set, its bonus reward and the day it was rolled for, so no two groups
 * need to be working on the same thing. There is no server-wide task list; the top-level
 * {@link #periodDay} exists only to notice the day turning over, which is when stale groups are
 * pruned.
 */
public class DailyTasksState extends SavedData {

    public static final String NAME = "roll_mod_daily_tasks";

    /**
     * The ceiling on how many tasks a group can hold at once — it sizes the arrays below, the
     * screen's rows and the command's index argument. How many a group <em>actually</em> draws is
     * its {@link api.DailyTaskQuota}, and is recorded as the length of {@link GroupState#taskIds}.
     */
    public static final int MAX_TASK_COUNT = 8;

    /**
     * The 06:00-shifted local date the server last noticed, as an epoch day.
     * {@link Long#MIN_VALUE} means "never seen a day yet". Groups carry their own copy; this one
     * only drives {@link #pruneStaleGroups}.
     */
    public long periodDay = Long.MIN_VALUE;

    /**
     * Bumped by every server-wide reroll and mixed into each group's draw seed. Without it a group
     * cleared by the reroll re-derives the very same set from world, group and day — the reroll
     * would change nothing. Saved, so a restart still re-derives the rerolled set.
     */
    public long rerollSalt;

    public final Map<UUID, GroupState> groups = new HashMap<>();

    /** One group's task set, bonus reward and progress for the day it was rolled on. */
    public static class GroupState {
        /**
         * The day this group's set was rolled for, as an epoch day. When it no longer matches the
         * current period the whole set is redrawn — see {@code DailyTaskManager.rollGroupIfNeeded}.
         * {@link Long#MIN_VALUE} means "never rolled", which is also what a group saved before
         * per-group rolls existed reads back as.
         */
        public long periodDay = Long.MIN_VALUE;

        /** This group's own task ids, drawn at its daily roll. Empty means "not rolled yet". */
        public final List<String> taskIds = new ArrayList<>();

        /**
         * The bonus reward for clearing all {@link #taskCount()} tasks, drawn at the same time.
         * Empty when nothing has been rolled, or when the id no longer resolves.
         */
        public String rewardId = "";

        public final int[] progress = new int[MAX_TASK_COUNT];
        /**
         * Sticky: once a task is done it stays done, even if the party grows and pushes the
         * required amount above the recorded progress.
         */
        public final boolean[] completed = new boolean[MAX_TASK_COUNT];
        /** Who has already taken each task's reward. Progress is shared; the payout is per player. */
        public final List<Set<UUID>> claimedBy = new ArrayList<>(MAX_TASK_COUNT);
        /** Who has already taken the all-complete bonus. Per player, for the same reason. */
        public final Set<UUID> rewardClaimedBy = new HashSet<>();

        public GroupState() {
            for (int i = 0; i < MAX_TASK_COUNT; i++) {
                claimedBy.add(new HashSet<>());
            }
        }

        /** Back to zero on every task and on the bonus, keeping the group's current task set. */
        public void clearProgress() {
            for (int i = 0; i < MAX_TASK_COUNT; i++) {
                progress[i] = 0;
                completed[i] = false;
                claimedBy.get(i).clear();
            }
            rewardClaimedBy.clear();
        }

        /**
         * How many tasks this group drew — its quota at the time of the roll, which is what the
         * bonus counts towards. Zero before the first roll.
         */
        public int taskCount() {
            return Math.min(taskIds.size(), MAX_TASK_COUNT);
        }

        /** How many of the day's tasks are finished — what the bonus panel counts. */
        public int completedCount() {
            int done = 0;
            for (int i = 0; i < taskCount(); i++) {
                if (completed[i]) done++;
            }
            return done;
        }
    }

    public GroupState group(UUID groupId) {
        return groups.computeIfAbsent(groupId, id -> new GroupState());
    }

    /**
     * Drops every group that is not on the current period. Their tasks would be redrawn and their
     * progress wiped on the next touch anyway, so keeping them only grows the save file — and
     * nothing else clears the map now that each group rolls on its own schedule.
     */
    public void pruneStaleGroups(long period) {
        if (groups.entrySet().removeIf(e -> e.getValue().periodDay != period)) {
            setDirty();
        }
    }

    public static DailyTasksState load(CompoundTag tag, HolderLookup.Provider provider) {
        DailyTasksState s = new DailyTasksState();
        s.periodDay = tag.contains("periodDay") ? tag.getLong("periodDay") : Long.MIN_VALUE;
        s.rerollSalt = tag.getLong("rerollSalt");

        ListTag groups = tag.getList("groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < groups.size(); i++) {
            CompoundTag g = groups.getCompound(i);
            UUID id = g.getUUID("id");
            GroupState state = new GroupState();

            // Absent in saves written before per-group rolls: those groups redraw on first touch.
            state.periodDay = g.contains("periodDay") ? g.getLong("periodDay") : Long.MIN_VALUE;
            state.rewardId = g.getString("rewardId");

            ListTag groupTasks = g.getList("taskIds", Tag.TAG_STRING);
            for (int t = 0; t < groupTasks.size(); t++) {
                state.taskIds.add(groupTasks.getString(t));
            }

            int[] progress = g.getIntArray("progress");
            System.arraycopy(progress, 0, state.progress, 0,
                    Math.min(progress.length, MAX_TASK_COUNT));

            byte[] completed = g.getByteArray("completed");
            for (int t = 0; t < Math.min(completed.length, MAX_TASK_COUNT); t++) {
                state.completed[t] = completed[t] != 0;
            }

            ListTag claimed = g.getList("claimed", Tag.TAG_LIST);
            for (int t = 0; t < Math.min(claimed.size(), MAX_TASK_COUNT); t++) {
                ListTag perTask = claimed.getList(t);
                for (int p = 0; p < perTask.size(); p++) {
                    state.claimedBy.get(t).add(NbtUtils.loadUUID(perTask.get(p)));
                }
            }

            ListTag rewardClaimed = g.getList("rewardClaimed", Tag.TAG_INT_ARRAY);
            for (int p = 0; p < rewardClaimed.size(); p++) {
                state.rewardClaimedBy.add(NbtUtils.loadUUID(rewardClaimed.get(p)));
            }

            s.groups.put(id, state);
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("periodDay", periodDay);
        tag.putLong("rerollSalt", rerollSalt);

        ListTag groupList = new ListTag();
        groups.forEach((id, state) -> {
            CompoundTag g = new CompoundTag();
            g.putUUID("id", id);
            g.putLong("periodDay", state.periodDay);
            g.putString("rewardId", state.rewardId);

            ListTag groupTasks = new ListTag();
            for (String taskId : state.taskIds) {
                groupTasks.add(StringTag.valueOf(taskId));
            }
            g.put("taskIds", groupTasks);

            g.putIntArray("progress", state.progress);

            byte[] completed = new byte[MAX_TASK_COUNT];
            for (int t = 0; t < MAX_TASK_COUNT; t++) {
                completed[t] = (byte) (state.completed[t] ? 1 : 0);
            }
            g.putByteArray("completed", completed);

            ListTag claimed = new ListTag();
            for (int t = 0; t < MAX_TASK_COUNT; t++) {
                ListTag perTask = new ListTag();
                for (UUID player : state.claimedBy.get(t)) {
                    perTask.add(NbtUtils.createUUID(player));
                }
                claimed.add(perTask);
            }
            g.put("claimed", claimed);

            ListTag rewardClaimed = new ListTag();
            for (UUID player : state.rewardClaimedBy) {
                rewardClaimed.add(NbtUtils.createUUID(player));
            }
            g.put("rewardClaimed", rewardClaimed);

            groupList.add(g);
        });
        tag.put("groups", groupList);
        return tag;
    }

    public static DailyTasksState get(ServerLevel overworld) {
        Factory<DailyTasksState> factory =
                new Factory<>(DailyTasksState::new, DailyTasksState::load);
        return overworld.getDataStorage().computeIfAbsent(factory, NAME);
    }
}
