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
    /** The code-defined tasks as scanned, before {@link #applySettings} lays amounts and made tasks over them. */
    private static List<DailyTask> scanned = List.of();
    private static Map<String, DailyTask> byId = Map.of();
    private static Map<String, Integer> indexById = Map.of();

    private DailyTaskRegistry() {}

    /** Runs once, from the mod's common setup. Safe to call again; the second call is ignored. */
    public static synchronized void bootstrap() {
        if (!tasks.isEmpty()) return;

        List<DailyTask> found = AutoScanner.scan(AutoDailyTask.class, DailyTask.class);
        // A task per Tide fish, made rather than annotated (there are 95).
        found.addAll(TideFishTasks.all());

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
        scanned = tasks;
        byId = Map.copyOf(ids);
        indexById = Map.copyOf(indices);

        // Logged from the list, not the map: this is the index order the GUI syncs against, and
        // Map.copyOf does not keep insertion order.
        RollMod.LOGGER.info("[DailyTasks] Registered {} task(s) in index order: {}",
                tasks.size(), tasks.stream().map(DailyTask::id).toList());
    }

    /**
     * Rebuilds the registry from the scanned tasks and the task settings ({@code TaskSettings}): a code task with an
     * amount set there asks for that amount instead, and the tasks made in the editor come after every code task,
     * sorted by id. Called with the same settings on the server and, from the synced copy, on every client, so an
     * index still means the same task on both sides; the code tasks keep their indices.
     */
    public static synchronized void applySettings(Map<String, Integer> amounts, List<? extends DailyTask> made) {
        applySettings(amounts, Map.of(), made);
    }

    /**
     * As above, and {@code multipliers} gives a code task another team multiplier: {@code 0} the usual
     * {@code base * (members + 1)}, {@code 1} the same for any party, more compounds per member.
     */
    public static synchronized void applySettings(Map<String, Integer> amounts, Map<String, Double> multipliers,
                                                  List<? extends DailyTask> made) {
        if (scanned.isEmpty()) return;
        List<DailyTask> out = new ArrayList<>(scanned.size() + made.size());
        for (DailyTask task : scanned) {
            Integer amount = amounts.get(task.id());
            Double mult = multipliers.get(task.id());
            boolean newAmount = amount != null && amount > 0 && amount != task.baseAmount();
            boolean newMult = mult != null && mult >= 0;
            out.add(newAmount || newMult ? new Amount(task, newAmount ? amount : task.baseAmount(),
                    newMult ? mult : Double.NaN) : task);
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (DailyTask task : scanned) seen.add(task.id());
        List<DailyTask> extra = new ArrayList<>();
        for (DailyTask task : made) {
            // A made task that needs a mod which is not here is left out, as bootstrap leaves out code tasks.
            if (task.requiredMod() != null && !ModList.get().isLoaded(task.requiredMod())) continue;
            if (seen.add(task.id())) extra.add(task);
        }
        extra.sort(Comparator.comparing(DailyTask::id));
        out.addAll(extra);

        Map<String, DailyTask> ids = new LinkedHashMap<>();
        Map<String, Integer> indices = new LinkedHashMap<>();
        for (int i = 0; i < out.size(); i++) {
            ids.put(out.get(i).id(), out.get(i));
            indices.put(out.get(i).id(), i);
        }
        tasks = List.copyOf(out);
        byId = Map.copyOf(ids);
        indexById = Map.copyOf(indices);
        java.util.Set<String> stats = new java.util.HashSet<>();
        for (DailyTask task : extra) {
            if (task.hook() == com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook.STAT
                    && task instanceof com.roll_54.roll_mod.minestar.dailytasks.tasks.ConfiguredTask c) {
                for (String t : c.targets()) if (!t.startsWith("#")) stats.add(t.contains(":") ? t : "minecraft:" + t);
            }
        }
        statTargets = java.util.Set.copyOf(stats);
    }

    private static java.util.Set<String> statTargets = java.util.Set.of();

    /** The statistics the made STAT tasks count; the only ones sampled. */
    public static java.util.Set<String> statTargets() {
        return statTargets;
    }

    /** The code-defined tasks, without the settings' amounts or the made tasks: what the editor lists as built in. */
    public static List<DailyTask> scanned() {
        return scanned;
    }

    /** A code task asking for another amount or team multiplier (NaN: its own); everything else is the task's own. */
    private record Amount(DailyTask task, int amount, double mult) implements DailyTask {
        @Override public String id() { return task.id(); }
        @Override public com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook hook() { return task.hook(); }
        @Override public int baseAmount() { return amount; }
        @Override public boolean scalesWithTeam() { return Double.isNaN(mult) ? task.scalesWithTeam() : mult != 1; }
        @Override public double teamMultiplier() { return Double.isNaN(mult) ? task.teamMultiplier() : mult; }
        @Override public boolean matches(Object subject) { return task.matches(subject); }
        @Override public boolean allowedFor(net.minecraft.server.level.ServerPlayer player) { return task.allowedFor(player); }
        @Override public net.minecraft.network.chat.Component name() { return task.name(); }
        @Override public net.minecraft.network.chat.Component tooltip(int required) { return task.tooltip(required); }
        @Override public String titleKey() { return task.titleKey(); }
        @Override public String descKey() { return task.descKey(); }
        @Override public com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon icon() { return task.icon(); }
        @Override public int weight() { return task.weight(); }
        @Override public String requiredMod() { return task.requiredMod(); }
        @Override public boolean battlePassOnly() { return task.battlePassOnly(); }
        @Override public net.minecraft.world.item.ItemStack toastIcon() { return task.toastIcon(); }
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
