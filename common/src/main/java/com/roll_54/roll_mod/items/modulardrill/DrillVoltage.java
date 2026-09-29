package com.roll_54.roll_mod.items.modulardrill;

/**
 * The five modular drill tiers and everything a tier decides on its own: how many module slots the
 * drill offers, the complexity budget those modules share, how much energy the bare drill holds, and what a single block costs to break
 * before any module multipliers.
 *
 * <p>Capacities mirror the old fixed drills they replace. The IV base cost is far below the old IV
 * pickaxe's 36k because that number paid for a built-in 11×11×11 — a bare modular drill mines a
 * single block, and the AOE module brings its own cost multiplier.
 */
public enum DrillVoltage {
    LV(3, 1_000_000L, 500, 6),
    MV(5, 10_000_000L, 1_000, 12),
    HV(6, 1_000_000_000L, 2_000, 18),
    EV(7, 50_000_000_000L, 4_000, 24),
    IV(7, 1_000_000_000_000L, 8_000, 30);

    public final int slots;
    public final long baseCapacity;
    public final long baseCostPerBlock;
    /** The complexity budget its modules may add up to. */
    public final int complexity;

    DrillVoltage(int slots, long baseCapacity, long baseCostPerBlock, int complexity) {
        this.slots = slots;
        this.baseCapacity = baseCapacity;
        this.baseCostPerBlock = baseCostPerBlock;
        this.complexity = complexity;
    }
}
