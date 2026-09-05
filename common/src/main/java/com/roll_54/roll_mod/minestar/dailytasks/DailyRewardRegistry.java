package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.AutoScanner;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every {@link DailyReward} in the game, found rather than declared — the reward counterpart of
 * {@link DailyTaskRegistry}, and built the same way for the same reasons.
 *
 * <p>{@link #bootstrap()} scans all loaded mod files for {@link AutoDailyReward} and instantiates
 * what it finds, so a new reward is one annotated class and nothing else.
 *
 * <p>The registry is built identically on the client and the server. That is load-bearing: the
 * daily-tasks screen syncs only a reward's <em>index</em> in {@link #all()}, and the client resolves
 * the name, tooltip and icon locally. Hence the deterministic sort by id — an index must mean the
 * same reward on both sides.
 */
public final class DailyRewardRegistry {

    private static List<DailyReward> rewards = List.of();
    private static Map<String, DailyReward> byId = Map.of();
    private static Map<String, Integer> indexById = Map.of();

    private DailyRewardRegistry() {}

    /** Runs once, from the mod's common setup. Safe to call again; the second call is ignored. */
    public static synchronized void bootstrap() {
        if (!rewards.isEmpty()) return;

        List<DailyReward> found = AutoScanner.scan(AutoDailyReward.class, DailyReward.class);

        // Deterministic order: the roll seeds off it, and the GUI syncs positions in this list.
        found.sort(Comparator.comparing(DailyReward::id));

        Map<String, DailyReward> ids = new LinkedHashMap<>();
        Map<String, Integer> indices = new LinkedHashMap<>();
        List<DailyReward> accepted = new ArrayList<>();
        for (DailyReward reward : found) {
            if (ids.putIfAbsent(reward.id(), reward) != null) {
                RollMod.LOGGER.error("[DailyTasks] Duplicate reward id '{}' from {}; ignoring the later one.",
                        reward.id(), reward.getClass().getName());
                continue;
            }
            if (reward.weight() <= 0) {
                RollMod.LOGGER.error("[DailyTasks] Reward '{}' has a non-positive weight; ignoring it.",
                        reward.id());
                ids.remove(reward.id());
                continue;
            }
            indices.put(reward.id(), accepted.size());
            accepted.add(reward);
        }

        rewards = List.copyOf(accepted);
        byId = Map.copyOf(ids);
        indexById = Map.copyOf(indices);

        // Logged from the list, not the map: this is the index order the GUI syncs against, and
        // Map.copyOf does not keep insertion order.
        RollMod.LOGGER.info("[DailyTasks] Registered {} reward(s) in index order: {}",
                rewards.size(), rewards.stream().map(DailyReward::id).toList());
    }

    /** Every registered reward, in a stable order shared by client and server. */
    public static List<DailyReward> all() {
        return rewards;
    }

    /** {@code null} when a saved id no longer exists, e.g. after a reward class is deleted. */
    @Nullable
    public static DailyReward byId(String id) {
        return byId.get(id);
    }

    /** The reward's position in {@link #all()}, or {@code -1}. This is what the GUI syncs. */
    public static int indexOf(String id) {
        return indexById.getOrDefault(id, -1);
    }

    /** {@code null} when the index is out of range — including the {@code -1} "no reward" marker. */
    @Nullable
    public static DailyReward byIndex(int index) {
        return index >= 0 && index < rewards.size() ? rewards.get(index) : null;
    }
}
