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
 * <p>Progress is keyed by <em>group</em>, not by player: a group is an FTB party (keyed by its team
 * id) or, for players in no party, the player themself (keyed by their own UUID). See
 * {@link DailyTaskGroups}.
 */
public class DailyTasksState extends SavedData {

    public static final String NAME = "roll_mod_daily_tasks";

    /** How many tasks are active at once. */
    public static final int TASK_COUNT = 4;

    /**
     * The 06:00-shifted local date the current task set was rolled for, as an epoch day.
     * {@link Long#MIN_VALUE} means "never rolled".
     */
    public long periodDay = Long.MIN_VALUE;

    /**
     * The day's default task ids, rolled at 06:00. New groups inherit this set, so by default
     * everybody on the server is working on the same four tasks. Empty until the first roll.
     */
    public final List<String> taskIds = new ArrayList<>();

    public final Map<UUID, GroupState> groups = new HashMap<>();

    /** One group's task set and progress for the current day. */
    public static class GroupState {
        /**
         * This group's own task ids. Seeded from {@link DailyTasksState#taskIds} the first time
         * the group is touched, and only diverges from it when an admin rerolls this group
         * specifically. Empty means "not seeded yet".
         */
        public final List<String> taskIds = new ArrayList<>();

        public final int[] progress = new int[TASK_COUNT];
        /**
         * Sticky: once a task is done it stays done, even if the party grows and pushes the
         * required amount above the recorded progress.
         */
        public final boolean[] completed = new boolean[TASK_COUNT];
        /** Who has already taken the reward. Progress is shared; the payout is per player. */
        public final List<Set<UUID>> claimedBy = new ArrayList<>(TASK_COUNT);

        public GroupState() {
            for (int i = 0; i < TASK_COUNT; i++) {
                claimedBy.add(new HashSet<>());
            }
        }

        /** Back to zero on every task, keeping the group's current task set. */
        public void clearProgress() {
            for (int i = 0; i < TASK_COUNT; i++) {
                progress[i] = 0;
                completed[i] = false;
                claimedBy.get(i).clear();
            }
        }
    }

    public GroupState group(UUID groupId) {
        return groups.computeIfAbsent(groupId, id -> new GroupState());
    }

    /** Wipes everyone's progress. Called by the daily roll. */
    public void resetProgress() {
        groups.clear();
    }

    public static DailyTasksState load(CompoundTag tag, HolderLookup.Provider provider) {
        DailyTasksState s = new DailyTasksState();
        s.periodDay = tag.contains("periodDay") ? tag.getLong("periodDay") : Long.MIN_VALUE;

        ListTag ids = tag.getList("taskIds", Tag.TAG_STRING);
        for (int i = 0; i < ids.size(); i++) {
            s.taskIds.add(ids.getString(i));
        }

        ListTag groups = tag.getList("groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < groups.size(); i++) {
            CompoundTag g = groups.getCompound(i);
            UUID id = g.getUUID("id");
            GroupState state = new GroupState();

            ListTag groupTasks = g.getList("taskIds", Tag.TAG_STRING);
            for (int t = 0; t < groupTasks.size(); t++) {
                state.taskIds.add(groupTasks.getString(t));
            }

            int[] progress = g.getIntArray("progress");
            System.arraycopy(progress, 0, state.progress, 0,
                    Math.min(progress.length, TASK_COUNT));

            byte[] completed = g.getByteArray("completed");
            for (int t = 0; t < Math.min(completed.length, TASK_COUNT); t++) {
                state.completed[t] = completed[t] != 0;
            }

            ListTag claimed = g.getList("claimed", Tag.TAG_LIST);
            for (int t = 0; t < Math.min(claimed.size(), TASK_COUNT); t++) {
                ListTag perTask = claimed.getList(t);
                for (int p = 0; p < perTask.size(); p++) {
                    state.claimedBy.get(t).add(NbtUtils.loadUUID(perTask.get(p)));
                }
            }

            s.groups.put(id, state);
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("periodDay", periodDay);

        ListTag ids = new ListTag();
        for (String id : taskIds) {
            ids.add(StringTag.valueOf(id));
        }
        tag.put("taskIds", ids);

        ListTag groupList = new ListTag();
        groups.forEach((id, state) -> {
            CompoundTag g = new CompoundTag();
            g.putUUID("id", id);

            ListTag groupTasks = new ListTag();
            for (String taskId : state.taskIds) {
                groupTasks.add(StringTag.valueOf(taskId));
            }
            g.put("taskIds", groupTasks);

            g.putIntArray("progress", state.progress);

            byte[] completed = new byte[TASK_COUNT];
            for (int t = 0; t < TASK_COUNT; t++) {
                completed[t] = (byte) (state.completed[t] ? 1 : 0);
            }
            g.putByteArray("completed", completed);

            ListTag claimed = new ListTag();
            for (int t = 0; t < TASK_COUNT; t++) {
                ListTag perTask = new ListTag();
                for (UUID player : state.claimedBy.get(t)) {
                    perTask.add(NbtUtils.createUUID(player));
                }
                claimed.add(perTask);
            }
            g.put("claimed", claimed);

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
