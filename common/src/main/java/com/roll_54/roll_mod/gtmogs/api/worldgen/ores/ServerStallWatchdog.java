package com.roll_54.roll_mod.gtmogs.api.worldgen.ores;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Watches the server thread from a separate daemon thread and dumps stacks when it stops ticking.
 *
 * <p>
 * {@link OreGenDiagnostics}'s periodic summary only runs while ore generation is making progress, which is
 * exactly what stops happening during a stall. This watchdog runs independently, so it still reports when
 * the server thread is blocked — the state where the server logs "Can't keep up!" and profilers time out
 * waiting for the main thread.
 *
 * <p>
 * The dump names the thread that is stuck and what it is stuck in, which is what distinguishes "world gen
 * is too slow and the server is waiting on a chunk" from a genuine lock cycle.
 *
 * <p>
 * With DimThreads the dimensions tick on their own threads ({@code dimthread_server_<dimension>}) while the server
 * thread waits for all of them. A stall then shows the server thread parked in DimThreads' pool; the dump starts with
 * the dimensions whose tick started and never finished (from a per-dimension heartbeat), and lists the dimension
 * threads before every other thread.
 */
public final class ServerStallWatchdog {

    private ServerStallWatchdog() {}

    /** How often the watchdog wakes up to compare the last tick against the stall threshold. */
    private static final long POLL_MILLIS = 2_000;

    /** Minimum gap between repeated dumps while a single stall is ongoing. */
    private static final long REPEAT_DUMP_NANOS = TimeUnit.SECONDS.toNanos(30);

    private static volatile Thread watcher;
    private static volatile Thread serverThread;
    private static volatile long lastTickNanos;
    private static volatile long stallStartedNanos;
    private static volatile long nextDumpNanos;

    /** The current or last tick of one dimension: which thread runs it, when it started, and when it ended (0: running). */
    private record LevelBeat(String thread, long startNanos, long endNanos) {}

    private static final Map<ResourceKey<Level>, LevelBeat> LEVEL_BEATS = new ConcurrentHashMap<>();

    /** Called at the start of every dimension's tick, on the thread that ticks it. */
    public static void onLevelTickStart(ServerLevel level) {
        LEVEL_BEATS.put(level.dimension(), new LevelBeat(Thread.currentThread().getName(), System.nanoTime(), 0L));
    }

    /** Called at the end of every dimension's tick, on the thread that ticks it. */
    public static void onLevelTickEnd(ServerLevel level) {
        LEVEL_BEATS.computeIfPresent(level.dimension(),
                (dimension, beat) -> new LevelBeat(beat.thread(), beat.startNanos(), System.nanoTime()));
    }

    public static void onServerTick(Thread ticking) {
        serverThread = ticking;
        lastTickNanos = System.nanoTime();

        long stallStart = stallStartedNanos;
        if (stallStart != 0L) {
            stallStartedNanos = 0L;
            nextDumpNanos = 0L;
            GTMOGS.LOGGER.warn("Server thread resumed after {}s stalled.",
                    TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - stallStart));
        }
    }

    public static synchronized void start() {
        if (watcher != null || stallThresholdSeconds() <= 0)
            return;

        lastTickNanos = System.nanoTime();
        stallStartedNanos = 0L;
        nextDumpNanos = 0L;

        Thread thread = new Thread(ServerStallWatchdog::run, "GTMOGS stall watchdog");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
        watcher = thread;

        GTMOGS.LOGGER.info("Stall watchdog running; will dump thread stacks if the server thread stops " +
                "ticking for {}s (worldgen.diagnostics.stallThresholdSeconds).", stallThresholdSeconds());
    }

    public static synchronized void stop() {
        Thread thread = watcher;
        watcher = null;
        serverThread = null;
        LEVEL_BEATS.clear();

        if (thread != null)
            thread.interrupt();
    }

    private static void run() {
        while (watcher == Thread.currentThread()) {
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            try {
                check();
            } catch (Exception e) {
                GTMOGS.LOGGER.error("Stall watchdog failed", e);
            }
        }
    }

    private static void check() {
        long thresholdSeconds = stallThresholdSeconds();
        if (thresholdSeconds <= 0)
            return;

        Thread ticking = serverThread;
        if (ticking == null || !ticking.isAlive())
            return;

        long now = System.nanoTime();
        long sinceTick = now - lastTickNanos;
        if (sinceTick < TimeUnit.SECONDS.toNanos(thresholdSeconds)) {
            return;
        }

        if (stallStartedNanos == 0L)
            stallStartedNanos = lastTickNanos;

        if (now < nextDumpNanos)
            return;
        nextDumpNanos = now + REPEAT_DUMP_NANOS;

        dump(ticking, TimeUnit.NANOSECONDS.toSeconds(sinceTick));
    }

    private static void dump(Thread ticking, long stalledSeconds) {
        StringBuilder report = new StringBuilder()
                .append("Server thread has not ticked for ").append(stalledSeconds).append("s.\n");

        String unfinished = describeUnfinishedLevels();
        if (unfinished != null) {
            report.append(unfinished).append('\n');
        }
        report.append(describeThread(ticking));

        String deadlocks = describeDeadlocks();
        if (deadlocks != null) {
            report.append("\nDeadlocked threads:").append(deadlocks);
        }

        report.append('\n').append(describeOtherThreads(ticking));
        report.append('\n').append(String.join("\n", OreGenDiagnostics.report()));

        GTMOGS.LOGGER.warn(report.toString());
    }

    /** Dimensions whose current tick started and has not ended, longest first; null when none. */
    private static String describeUnfinishedLevels() {
        long now = System.nanoTime();
        var running = LEVEL_BEATS.entrySet().stream()
                .filter(e -> e.getValue().endNanos() == 0L)
                .sorted(Comparator.comparingLong(e -> e.getValue().startNanos()))
                .toList();
        if (running.isEmpty())
            return null;

        StringBuilder builder = new StringBuilder("Dimension ticks still running:");
        for (var entry : running) {
            builder.append("\n  ").append(entry.getKey().location())
                    .append(" on \"").append(entry.getValue().thread()).append("\" for ")
                    .append(TimeUnit.NANOSECONDS.toSeconds(now - entry.getValue().startNanos())).append('s');
        }
        return builder.toString();
    }

    private static String describeThread(Thread thread) {
        StringBuilder builder = new StringBuilder()
                .append("  \"").append(thread.getName()).append("\" state=").append(thread.getState());

        for (StackTraceElement element : thread.getStackTrace()) {
            builder.append("\n\tat ").append(element);
        }
        return builder.toString();
    }

    /**
     * Dumps every other live thread, worldgen workers first.
     *
     * <p>
     * This deliberately does not filter to the "Worker-Main" pool. Not every stall is the server thread waiting on
     * chunk generation: it can also be blocked on a lock some unrelated thread holds -- a chunk section acquired
     * via {@code BulkSectionAccess} and never released will park the server thread in
     * {@code ThreadingDetector.checkAndLock} while it tries to save that chunk, with the worldgen pool sitting
     * idle. {@link #describeDeadlocks()} does not catch that either, since a leaked permit is not a lock cycle.
     * Filtering to worldgen workers discards the one stack that identifies the culprit.
     */
    private static String describeOtherThreads(Thread ticking) {
        var traces = new ArrayList<>(Thread.getAllStackTraces().entrySet());

        // DimThreads dimension threads first (the server thread waits on them), then worldgen workers, then whatever
        // is actually running or blocked, then the rest.
        traces.sort(Comparator
                .comparingInt((Map.Entry<Thread, StackTraceElement[]> e) -> {
                    if (e.getKey().getName().startsWith("dimthread_server_")) return 0;
                    if (e.getKey().getName().startsWith("Worker-Main")) return 1;
                    return switch (e.getKey().getState()) {
                        case BLOCKED -> 2;
                        case RUNNABLE -> 3;
                        default -> 4;
                    };
                })
                .thenComparing(e -> e.getKey().getName()));

        StringBuilder builder = new StringBuilder("Other threads:");
        boolean any = false;

        for (var entry : traces) {
            Thread thread = entry.getKey();
            if (thread == ticking || entry.getValue().length == 0)
                continue;

            any = true;
            builder.append("\n  \"").append(thread.getName()).append("\" state=").append(thread.getState());
            for (StackTraceElement element : entry.getValue()) {
                builder.append("\n\tat ").append(element);
            }
        }

        return any ? builder.toString() : "Other threads: none running";
    }

    private static String describeDeadlocks() {
        try {
            ThreadMXBean threads = ManagementFactory.getThreadMXBean();
            long[] deadlocked = threads.findDeadlockedThreads();
            if (deadlocked == null || deadlocked.length == 0)
                return null;

            StringBuilder builder = new StringBuilder();
            for (ThreadInfo info : threads.getThreadInfo(deadlocked, true, true)) {
                if (info != null)
                    builder.append('\n').append(info);
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static long stallThresholdSeconds() {
        var config = ConfigHolder.INSTANCE;
        return config == null ? 10 : config.worldgen.diagnostics.stallThresholdSeconds;
    }
}
