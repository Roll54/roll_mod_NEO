package com.roll_54.roll_mod_server.threading;

/**
 * Master switch and tunables for the per-world threading coremod.
 *
 * <p>Plan: {@code ~/.claude/plans/how-hard-is-to-wiggly-goblet.md}. The whole system ships
 * behind {@link #ENABLED}, which defaults to {@code false}: while disabled, none of the
 * threading machinery is installed and the server ticks exactly as vanilla. This lets the
 * infrastructure land (Stage 0) without changing any behavior.
 *
 * <p>Toggle at launch with a JVM flag, e.g. {@code -Droll_mod.worldThreading=true}. This is a
 * deliberately dependency-free feature flag; it can be migrated to the mod's config framework
 * once the behavior is proven.
 */
public final class WorldThreadingConfig {

    private WorldThreadingConfig() {}

    /** Property key controlling {@link #ENABLED}. */
    public static final String ENABLE_PROPERTY = "roll_mod.worldThreading";

    /**
     * When {@code false} (default) the coremod is fully inert. Enabling it activates per-world
     * ticking as implemented by the current stage.
     */
    public static final boolean ENABLED = Boolean.getBoolean(ENABLE_PROPERTY);

    /**
     * Target ticks-per-second each world thread free-runs at once desynced (Stage 5).
     * Vanilla is 20; kept configurable for testing.
     */
    public static final int TARGET_TPS =
            Integer.getInteger("roll_mod.worldThreading.tps", 20);

    /** Nanoseconds per tick derived from {@link #TARGET_TPS}. */
    public static final long NANOS_PER_TICK = 1_000_000_000L / Math.max(1, TARGET_TPS);
}
