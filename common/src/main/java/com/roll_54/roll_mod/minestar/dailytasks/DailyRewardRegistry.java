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
    /** The code-defined rewards found by {@link #bootstrap}, before any configured ones are laid over them. */
    private static List<DailyReward> scanned = List.of();
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

        scanned = List.copyOf(accepted);
        rewards = List.copyOf(accepted);
        byId = Map.copyOf(ids);
        indexById = Map.copyOf(indices);

        // Logged from the list, not the map: this is the index order the GUI syncs against, and
        // Map.copyOf does not keep insertion order.
        RollMod.LOGGER.info("[DailyTasks] Registered {} reward(s) in index order: {}",
                rewards.size(), rewards.stream().map(DailyReward::id).toList());
    }

    /**
     * Lays the configured bonuses ({@code daily_rewards.json}, the Moderation tab) over the code-defined ones: the
     * configured come first, in the config's order, then every code-defined reward whose id none of them took. Run on
     * the server when the config loads or is saved, and on a client from the server's synced copy, so the index the
     * screen syncs means the same reward on both sides.
     *
     * <p>A code-defined reward that is not configured stays registered (a group it was drawn for can still collect
     * it) but is no longer drawn: once a bonus list is configured, it is the whole draw.
     */
    public static synchronized void applyConfigured(List<? extends DailyReward> configured) {
        List<DailyReward> all = new ArrayList<>();
        Map<String, DailyReward> ids = new LinkedHashMap<>();
        Map<String, Integer> indices = new LinkedHashMap<>();
        for (DailyReward reward : configured) {
            if (ids.putIfAbsent(reward.id(), reward) != null) continue;
            indices.put(reward.id(), all.size());
            all.add(reward);
        }
        boolean replaced = !configured.isEmpty();
        for (DailyReward reward : scanned) {
            if (ids.containsKey(reward.id())) continue;
            DailyReward kept = replaced ? retired(reward) : reward;
            ids.put(reward.id(), kept);
            indices.put(reward.id(), all.size());
            all.add(kept);
        }
        rewards = List.copyOf(all);
        byId = Map.copyOf(ids);
        indexById = Map.copyOf(indices);
    }

    /** {@code reward}, still payable but never drawn. */
    private static DailyReward retired(DailyReward reward) {
        return new DailyReward() {
            @Override public String id() { return reward.id(); }
            @Override public com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon icon() { return reward.icon(); }
            @Override public List<com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon> icons() { return reward.icons(); }
            @Override public void grant(net.minecraft.server.level.ServerPlayer player) { reward.grant(player); }
            @Override public net.minecraft.network.chat.Component name() { return reward.name(); }
            @Override public net.minecraft.network.chat.Component tooltip() { return reward.tooltip(); }
            @Override public boolean drawable() { return false; }
            @Override public int weight() { return reward.weight(); }
        };
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
