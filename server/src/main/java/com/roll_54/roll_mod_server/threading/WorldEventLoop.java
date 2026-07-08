package com.roll_54.roll_mod_server.threading;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;

/**
 * A task queue bound to a single {@link ServerLevel}, executed on that world's dedicated
 * {@link WorldThread}. This is the <b>cross-world routing primitive</b>: any code running on
 * thread A that must touch world B enqueues the work via {@code worldB.execute(runnable)},
 * which lands here and runs on B's thread at its next tick boundary.
 *
 * <p>Modeled on vanilla's {@code ServerChunkCache.MainThreadExecutor}
 * (see {@code ServerChunkCache.java:529}), which is the same pattern for the chunk system.
 *
 * <p>Plan: {@code ~/.claude/plans/how-hard-is-to-wiggly-goblet.md}.
 */
public final class WorldEventLoop extends BlockableEventLoop<Runnable> {

    private final ServerLevel level;

    /**
     * The thread that owns this loop. Set once when the {@link WorldThread} starts; until then
     * {@link #isSameThread()} is false so tasks are safely enqueued rather than run inline.
     */
    private volatile Thread runningThread;

    public WorldEventLoop(ServerLevel level) {
        super("roll_mod-world-" + level.dimension().location());
        this.level = level;
    }

    /** Called by {@link WorldThread#run()} to claim ownership of this loop. */
    void bindThread(Thread thread) {
        this.runningThread = thread;
    }

    public ServerLevel level() {
        return this.level;
    }

    @Override
    protected Runnable wrapRunnable(Runnable runnable) {
        return runnable;
    }

    @Override
    protected boolean shouldRun(Runnable runnable) {
        // Cross-world tasks are always eligible to run when drained.
        return true;
    }

    @Override
    protected Thread getRunningThread() {
        return this.runningThread;
    }
}
