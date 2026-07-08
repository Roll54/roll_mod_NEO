package com.roll_54.roll_mod_server.threading;

import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the lifecycle of every {@link WorldThread} and the registry mapping each {@link ServerLevel}
 * to its thread/executor. In later stages this also runs the truly-global systems that survived the
 * split (player-list housekeeping, connection accept, scoreboard/boss-event actors, command
 * functions) and orchestrates the reload / save / shutdown <b>quiesce barriers</b> from the
 * synchronization contract.
 *
 * <p><b>Stage 0:</b> only the registry and routing helpers exist and nothing invokes
 * {@link #startAll()} / {@link #stopAll()} yet — the server still ticks levels the vanilla way.
 * Stage 1's {@code MinecraftServer} mixin begins driving this.
 *
 * <p>Plan: {@code ~/.claude/plans/how-hard-is-to-wiggly-goblet.md}.
 */
public final class GlobalCoordinator {

    private static final GlobalCoordinator INSTANCE = new GlobalCoordinator();

    public static GlobalCoordinator get() {
        return INSTANCE;
    }

    private GlobalCoordinator() {}

    /** ServerLevel -> its dedicated tick thread. Concurrent: read from many world threads. */
    private final Map<ServerLevel, WorldThread> threads = new ConcurrentHashMap<>();

    /** The tick thread for a level, or {@code null} if threading is off or not yet started. */
    public WorldThread threadFor(ServerLevel level) {
        return this.threads.get(level);
    }

    /**
     * Route work onto {@code level}'s thread. If threading is inactive for that level, the work
     * runs inline on the caller — preserving vanilla single-thread semantics while disabled.
     */
    public void executeOn(ServerLevel level, Runnable task) {
        WorldThread thread = this.threads.get(level);
        if (thread != null) {
            thread.loop().execute(task);
        } else {
            task.run();
        }
    }

    /**
     * Create and start one {@link WorldThread} per loaded level.
     * TODO(stage-1): enumerate {@code server.levels} (widened via AT) and populate {@link #threads},
     * driven by the {@code MinecraftServer} tick mixin, gated on {@link WorldThreadingConfig#ENABLED}.
     */
    public void startAll() {
        // Intentionally empty in Stage 0.
    }

    /**
     * Stop and join every world thread.
     * TODO(stage-5/6): signal {@link WorldThread#requestStop()}, join with a watchdog timeout.
     */
    public void stopAll() {
        // Intentionally empty in Stage 0.
    }
}
