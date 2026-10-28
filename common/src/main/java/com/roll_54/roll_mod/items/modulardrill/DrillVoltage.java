package com.roll_54.roll_mod.items.modulardrill;

/**
 * The five modular drill tiers and everything a tier decides on its own: how many module slots the
 * drill offers, the complexity budget those modules share (see {@link #complexity()}), how much energy
 * the bare drill holds, and what a single block costs to break
 * before any module multipliers.
 *
 * <p>Capacities mirror the old fixed drills they replace. The IV base cost is far below the old IV
 * pickaxe's 36k because that number paid for a built-in 11×11×11 — a bare modular drill mines a
 * single block, and the AOE module brings its own cost multiplier.
 */
public enum DrillVoltage {
    // The LV drill gets one complexity over the formula, so its 3 slots fit more than two tier-I modules.
    LV(3, 1_000_000L, 500, 1),
    MV(5, 10_000_000L, 1_000, 0),
    HV(6, 1_000_000_000L, 2_000, 0),
    EV(7, 50_000_000_000L, 4_000, 0),
    IV(7, 1_000_000_000_000L, 8_000, 0);

    public final int slots;
    public final long baseCapacity;
    public final long baseCostPerBlock;
    /** Complexity on top of {@link ModuleComplexity#budget}, for this tier alone. */
    public final int extraComplexity;

    DrillVoltage(int slots, long baseCapacity, long baseCostPerBlock, int extraComplexity) {
        this.slots = slots;
        this.baseCapacity = baseCapacity;
        this.baseCostPerBlock = baseCostPerBlock;
        this.extraComplexity = extraComplexity;
    }

    /** The complexity budget its modules may add up to — see {@link ModuleComplexity}. */
    public int complexity() {
        return ModuleComplexity.budget(ordinal() + 1) + extraComplexity;
    }
}
