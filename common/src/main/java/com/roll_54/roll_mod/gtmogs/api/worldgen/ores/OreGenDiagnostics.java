package com.roll_54.roll_mod.gtmogs.api.worldgen.ores;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lightweight instrumentation for the ore generation pipeline.
 *
 * <p>
 * Ore generation runs on world gen worker threads, where a slow vein shows up as "Can't keep up!" on the
 * server thread rather than as anything pointing at this mod. This tracks per-phase timings, warns about
 * individual chunks that take too long, and periodically summarises where the time went.
 *
 * <p>
 * Enabled by default and designed to stay cheap when nothing is wrong: two {@code nanoTime} calls and a
 * couple of atomic adds per chunk phase, no allocation on the hot path.
 */
public final class OreGenDiagnostics {

    private OreGenDiagnostics() {}

    public enum Phase {

        /** Rolling which veins exist around a chunk, and where their centers are. */
        VEIN_METADATA("vein metadata"),
        /** Running a vein generator to produce its block placers. */
        VEIN_SHAPE("vein shape"),
        /** Running indicator generators for a vein. */
        INDICATORS("indicators"),
        /** Writing a chunk's share of the surrounding veins' ore blocks. */
        PLACE_ORES("place ores"),
        /** Writing a chunk's share of the surrounding veins' indicator blocks. */
        PLACE_INDICATORS("place indicators");

        private final String label;

        Phase(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final Phase[] PHASES = Phase.values();

    private static final LongAdder[] COUNTS = newAdders();
    private static final LongAdder[] TOTAL_NANOS = newAdders();
    private static final AtomicLong[] MAX_NANOS = newCounters();

    private static final AtomicLong SLOW_CHUNKS = new AtomicLong();
    private static final AtomicLong NEXT_SLOW_WARN_NANOS = new AtomicLong();
    private static final AtomicLong NEXT_SUMMARY_NANOS = new AtomicLong();
    private static final AtomicLong STARTED_NANOS = new AtomicLong();

    /** Minimum gap between individual slow-chunk warnings, so a bad vein can't flood the log. */
    private static final long SLOW_WARN_COOLDOWN_NANOS = TimeUnit.SECONDS.toNanos(10);

    /** The biggest veins by cluster size, as of the last datapack load. These drive the search radius. */
    private static volatile List<String> LARGEST_VEINS = List.of();

    private static int averageClusterSize(OreVeinDefinition definition) {
        var clusterSize = definition.clusterSize();
        return (clusterSize.getMinValue() + clusterSize.getMaxValue()) / 2;
    }

    public static boolean enabled() {
        var config = ConfigHolder.INSTANCE;
        return config == null || config.worldgen.diagnostics.enabled;
    }

    /**
     * @return A timestamp to hand back to {@link #record}, or {@code 0} when diagnostics are off.
     */
    public static long start() {
        return enabled() ? System.nanoTime() : 0L;
    }

    /**
     * Records one phase invocation. No-op if {@code startNanos} is 0.
     */
    public static void record(Phase phase, long startNanos) {
        if (startNanos == 0L)
            return;

        long elapsed = System.nanoTime() - startNanos;
        int index = phase.ordinal();

        COUNTS[index].increment();
        TOTAL_NANOS[index].add(elapsed);
        MAX_NANOS[index].accumulateAndGet(elapsed, Math::max);
        STARTED_NANOS.compareAndSet(0L, startNanos);

        maybeLogSummary();
    }

    /**
     * Records a phase that operated on a specific chunk, warning when it took longer than
     * {@code slowChunkMillis}.
     *
     * @param veins How many veins the chunk pulled in — usually the reason a chunk is slow.
     */
    public static void recordChunk(Phase phase, long startNanos, WorldGenLevel level, ChunkPos chunkPos, int veins) {
        if (startNanos == 0L)
            return;

        long elapsed = System.nanoTime() - startNanos;
        record(phase, startNanos);

        long thresholdNanos = TimeUnit.MILLISECONDS.toNanos(slowChunkMillis());
        if (elapsed < thresholdNanos)
            return;

        SLOW_CHUNKS.incrementAndGet();

        long now = System.nanoTime();
        long nextWarn = NEXT_SLOW_WARN_NANOS.get();
        if (now < nextWarn || !NEXT_SLOW_WARN_NANOS.compareAndSet(nextWarn, now + SLOW_WARN_COOLDOWN_NANOS))
            return;

        GTMOGS.LOGGER.warn(
                "Slow ore gen: {} took {} for chunk {} in {} ({} veins in range, scanning {} chunks each way). " +
                        "Reduce cluster_size / oreVeinRandomOffset, or raise oreVeinGridSize, if this repeats.",
                phase.label(), formatNanos(elapsed), chunkPos, dimensionName(level), veins,
                OreVeinUtil.getMaxVeinSearchDistance());
    }

    /**
     * Logs the derived world gen costs once veins are known — the single most useful number for diagnosing
     * slow chunk generation, since every decorated chunk scans this many neighbours.
     *
     * <p>
     * The search radius is driven by the <em>largest</em> vein in the pack, and applies to every dimension, so
     * the biggest veins are listed by name: one oversized {@code cluster_size} anywhere makes every chunk
     * everywhere more expensive.
     */
    public static void logVeinSearchCost(Registry<OreVeinDefinition> veins) {
        if (!enabled())
            return;

        LARGEST_VEINS = veins.holders()
                .filter(holder -> holder.value().canGenerate())
                .sorted(Comparator.comparingInt(
                        (Holder.Reference<OreVeinDefinition> holder) -> averageClusterSize(holder.value()))
                        .reversed())
                .limit(5)
                .map(holder -> "%s (%d blocks)".formatted(holder.key().location(),
                        averageClusterSize(holder.value())))
                .toList();

        if (!LARGEST_VEINS.isEmpty()) {
            GTMOGS.LOGGER.info("Largest ore veins: {}", String.join(", ", LARGEST_VEINS));
        }

        int searchDistance = OreVeinUtil.getMaxVeinSearchDistance();
        int scannedChunks = OreVeinUtil.getOriginChunksPerGeneratedChunk();

        String message = "Ore vein search radius is {} chunk(s): every generated chunk resolves veins for up to {} " +
                "grid-aligned origin chunks (largest vein {} blocks, grid size {}, random offset {}).";
        Object[] args = { searchDistance, scannedChunks, GTOreVeins.getLargestVeinSize(),
                ConfigHolder.INSTANCE.worldgen.oreVeins.oreVeinGridSize,
                ConfigHolder.INSTANCE.worldgen.oreVeins.oreVeinRandomOffset };

        if (scannedChunks > 121) {
            GTMOGS.LOGGER.warn(message + " That is a lot of work per chunk; a single oversized vein is usually " +
                    "the cause.", args);
        } else {
            GTMOGS.LOGGER.info(message, args);
        }
    }

    /**
     * Renders the accumulated stats, e.g. for {@code /gtmogs diagnostics}.
     */
    public static List<String> report() {
        List<String> lines = new ArrayList<>();
        long started = STARTED_NANOS.get();
        long window = started == 0L ? 0L : System.nanoTime() - started;

        lines.add("GTMOGS ore gen — %s of samples, %d slow chunk(s) over %dms"
                .formatted(formatNanos(window), SLOW_CHUNKS.get(), slowChunkMillis()));

        for (Phase phase : PHASES) {
            long count = COUNTS[phase.ordinal()].sum();
            if (count == 0) {
                lines.add("  %-16s never ran".formatted(phase.label()));
                continue;
            }

            long total = TOTAL_NANOS[phase.ordinal()].sum();
            lines.add("  %-16s %6d calls, total %s, avg %s, worst %s".formatted(
                    phase.label(), count, formatNanos(total), formatNanos(total / count),
                    formatNanos(MAX_NANOS[phase.ordinal()].get())));
        }

        int searchDistance = OreVeinUtil.getMaxVeinSearchDistance();
        lines.add("  search radius %d chunk(s) → up to %d grid-aligned origin chunks resolved per generated chunk"
                .formatted(searchDistance, OreVeinUtil.getOriginChunksPerGeneratedChunk()));
        lines.add("  largest vein %d blocks".formatted(GTOreVeins.getLargestVeinSize()));
        if (!LARGEST_VEINS.isEmpty()) {
            lines.add("  biggest veins: " + String.join(", ", LARGEST_VEINS));
        }

        String deadlocks = describeDeadlocks();
        lines.add(deadlocks == null ? "  no deadlocked threads" : "  DEADLOCK: " + deadlocks);

        return lines;
    }

    public static void reset() {
        for (int i = 0; i < PHASES.length; i++) {
            COUNTS[i].reset();
            TOTAL_NANOS[i].reset();
            MAX_NANOS[i].set(0L);
        }
        SLOW_CHUNKS.set(0L);
        STARTED_NANOS.set(0L);
    }

    private static void maybeLogSummary() {
        long intervalNanos = TimeUnit.SECONDS.toNanos(summaryIntervalSeconds());
        if (intervalNanos <= 0)
            return;

        long now = System.nanoTime();
        long next = NEXT_SUMMARY_NANOS.get();

        if (next == 0L) {
            NEXT_SUMMARY_NANOS.compareAndSet(0L, now + intervalNanos);
            return;
        }

        // Only the thread that wins the CAS logs, so concurrent workers can't duplicate the summary.
        if (now < next || !NEXT_SUMMARY_NANOS.compareAndSet(next, now + intervalNanos))
            return;

        report().forEach(GTMOGS.LOGGER::info);
        reset();
    }

    /**
     * @return A description of any deadlocked threads, or {@code null} when there are none.
     */
    private static String describeDeadlocks() {
        if (ConfigHolder.INSTANCE != null && !ConfigHolder.INSTANCE.worldgen.diagnostics.detectDeadlocks)
            return null;

        try {
            ThreadMXBean threads = ManagementFactory.getThreadMXBean();
            long[] deadlocked = threads.findDeadlockedThreads();
            if (deadlocked == null || deadlocked.length == 0)
                return null;

            StringBuilder builder = new StringBuilder();
            for (ThreadInfo info : threads.getThreadInfo(deadlocked, true, true)) {
                if (info == null)
                    continue;
                builder.append('\n').append(info);
            }
            return builder.toString();
        } catch (Exception e) {
            // Monitoring can be unavailable/restricted; never let diagnostics break world gen.
            return null;
        }
    }

    private static long slowChunkMillis() {
        var config = ConfigHolder.INSTANCE;
        return config == null ? 100 : config.worldgen.diagnostics.slowChunkMillis;
    }

    private static long summaryIntervalSeconds() {
        var config = ConfigHolder.INSTANCE;
        return config == null ? 300 : config.worldgen.diagnostics.summaryIntervalSeconds;
    }

    private static String dimensionName(WorldGenLevel level) {
        return level.getLevel().dimension().location().toString();
    }

    private static String formatNanos(long nanos) {
        if (nanos < 1_000_000L)
            return "%.2fms".formatted(nanos / 1_000_000.0);
        if (nanos < TimeUnit.SECONDS.toNanos(1))
            return "%.1fms".formatted(nanos / 1_000_000.0);
        return "%.2fs".formatted(nanos / 1_000_000_000.0);
    }

    private static LongAdder[] newAdders() {
        LongAdder[] adders = new LongAdder[Phase.values().length];
        for (int i = 0; i < adders.length; i++) {
            adders[i] = new LongAdder();
        }
        return adders;
    }

    private static AtomicLong[] newCounters() {
        AtomicLong[] counters = new AtomicLong[Phase.values().length];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = new AtomicLong();
        }
        return counters;
    }
}
