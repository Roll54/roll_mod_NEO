package com.roll_54.roll_mod_server.threading;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.locks.LockSupport;

/**
 * The dedicated thread that ticks one {@link ServerLevel}. In the desynced end state each
 * instance free-runs its own {@value WorldThreadingConfig#TARGET_TPS}-TPS loop: tick the world,
 * drain any cross-world tasks routed to it, then park until the next tick boundary. A slow world
 * only slows its own thread — never the others.
 *
 * <p><b>Stage 0:</b> this class compiles and is fully wired to its {@link WorldEventLoop}, but
 * nothing constructs or {@link #start()}s it yet, and {@link #tickWorld()} is intentionally a
 * no-op. The {@code GlobalCoordinator} takes over lifecycle in Stage 1, and {@link #tickWorld()}
 * gains the real {@code level.tick(...)} call there. Until then the server ticks exactly as vanilla.
 *
 * <p>Plan: {@code ~/.claude/plans/how-hard-is-to-wiggly-goblet.md}.
 */
public final class WorldThread extends Thread {

    private final ServerLevel level;
    private final WorldEventLoop loop;
    private volatile boolean running = true;

    public WorldThread(ServerLevel level) {
        super("roll_mod-world-" + level.dimension().location());
        this.setDaemon(false);
        this.level = level;
        this.loop = new WorldEventLoop(level);
    }

    /** The cross-world task executor for this world. Enqueue work here to run it on this thread. */
    public WorldEventLoop loop() {
        return this.loop;
    }

    public ServerLevel level() {
        return this.level;
    }

    /** Signal the loop to stop; join separately after (see coordinator shutdown, Stage 5/6). */
    public void requestStop() {
        this.running = false;
        LockSupport.unpark(this);
    }

    @Override
    public void run() {
        this.loop.bindThread(this);
        RollMod.LOGGER.info("[world-threading] started thread for {}", level.dimension().location());
        try {
            while (this.running) {
                long tickStartNanos = System.nanoTime();

                tickWorld();
                drainCrossWorldTasks();

                parkUntilNextTick(tickStartNanos);
            }
        } catch (Throwable t) {
            // Stage 6 adds proper per-world crash isolation (unload this world, keep the server up).
            RollMod.LOGGER.error("[world-threading] fatal error on {}, thread exiting",
                    level.dimension().location(), t);
        } finally {
            RollMod.LOGGER.info("[world-threading] stopped thread for {}", level.dimension().location());
        }
    }

    /**
     * Tick this world once. No-op in Stage 0; Stage 1 wires the real {@code level.tick(hasTimeLeft)}
     * (dispatched from the coordinator with a barrier), Stage 5 removes the barrier for full desync.
     */
    private void tickWorld() {
        // TODO(stage-1): level.tick(() -> false) equivalent, guarded by tick-rate manager.
    }

    /** Run every cross-world task that has been routed to this world since the last drain. */
    private void drainCrossWorldTasks() {
        while (this.loop.pollTask()) {
            // BlockableEventLoop#pollTask runs one queued task per call.
        }
    }

    /** Sleep the remainder of this world's tick budget so it holds its target TPS. */
    private void parkUntilNextTick(long tickStartNanos) {
        long elapsed = System.nanoTime() - tickStartNanos;
        long remaining = WorldThreadingConfig.NANOS_PER_TICK - elapsed;
        if (remaining > 0) {
            LockSupport.parkNanos(remaining);
        }
    }
}
