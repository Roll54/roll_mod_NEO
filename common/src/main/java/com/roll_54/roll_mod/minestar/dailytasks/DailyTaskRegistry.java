package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.AutoScanner;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every {@link DailyTask} in the game, found rather than declared.
 *
 * <p>{@link #bootstrap()} scans all loaded mod files for {@link AutoDailyTask} and instantiates
 * what it finds — so a new task is one annotated class and nothing else, and another mod can
 * contribute tasks without this one knowing about it. The scan itself lives in {@link AutoScanner},
 * shared with {@link DailyRewardRegistry}.
 *
 * <p>The registry is built identically on the client and the server. That is load-bearing: the
 * daily-tasks screen syncs only a task's <em>index</em> in {@link #all()}, and the client resolves
 * the name, tooltip and icon locally. Hence the deterministic sort by id — an index must mean the
 * same task on both sides.
 */
public final class DailyTaskRegistry {

    private static List<DailyTask> tasks = List.of();
    private static Map<String, DailyTask> byId = Map.of();
    private static Map<String, Integer> indexById = Map.of();

    private DailyTaskRegistry() {}

    /** Runs once, from the mod's common setup. Safe to call again; the second call is ignored. */
    public static synchronized void bootstrap() {
        if (!tasks.isEmpty()) return;

        List<DailyTask> found = AutoScanner.scan(AutoDailyTask.class, DailyTask.class);

        // Deterministic order: the roll seeds off it, and the GUI syncs positions in this list.
        found.sort(Comparator.comparing(DailyTask::id));

        Map<String, DailyTask> ids = new LinkedHashMap<>();
        Map<String, Integer> indices = new LinkedHashMap<>();
        List<DailyTask> accepted = new ArrayList<>();
        for (DailyTask task : found) {
            String requiredMod = task.requiredMod();
            if (requiredMod != null && !ModList.get().isLoaded(requiredMod)) {
                RollMod.LOGGER.info("[DailyTasks] Task '{}' needs '{}', which is not loaded; skipping it.",
                        task.id(), requiredMod);
                continue;
            }
            if (ids.putIfAbsent(task.id(), task) != null) {
                RollMod.LOGGER.error("[DailyTasks] Duplicate task id '{}' from {}; ignoring the later one.",
                        task.id(), task.getClass().getName());
                continue;
            }
            if (task.baseAmount() <= 0 || task.weight() <= 0) {
                RollMod.LOGGER.error("[DailyTasks] Task '{}' has a non-positive baseAmount/weight; ignoring it.",
                        task.id());
                ids.remove(task.id());
                continue;
            }
            indices.put(task.id(), accepted.size());
            accepted.add(task);
        }

        tasks = List.copyOf(accepted);
        byId = Map.copyOf(ids);
        indexById = Map.copyOf(indices);

        // Logged from the list, not the map: this is the index order the GUI syncs against, and
        // Map.copyOf does not keep insertion order.
        RollMod.LOGGER.info("[DailyTasks] Registered {} task(s) in index order: {}",
                tasks.size(), tasks.stream().map(DailyTask::id).toList());
    }

    /** Every registered task, in a stable order shared by client and server. */
    public static List<DailyTask> all() {
        return tasks;
    }

    /** {@code null} when a saved id no longer exists, e.g. after a task class is deleted. */
    @Nullable
    public static DailyTask byId(String id) {
        return byId.get(id);
    }

    /** The task's position in {@link #all()}, or {@code -1}. This is what the GUI syncs. */
    public static int indexOf(String id) {
        return indexById.getOrDefault(id, -1);
    }

    /** {@code null} when the index is out of range — including the {@code -1} "no task" marker. */
    @Nullable
    public static DailyTask byIndex(int index) {
        return index >= 0 && index < tasks.size() ? tasks.get(index) : null;
    }
}
