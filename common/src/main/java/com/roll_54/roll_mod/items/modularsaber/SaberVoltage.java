package com.roll_54.roll_mod.items.modularsaber;

/**
 * The five modular saber tiers and everything a tier decides on its own: module slots, how much
 * energy the bare saber holds, how hard it hits while switched on, what one hit costs before any
 * module multipliers, and what it drains every second just for being switched on.
 *
 * <p>Damage climbs linearly, +5 per tier, so modules — not the tier alone — make the big numbers.
 * Capacity and per-hit cost carry over from the fixed sabers these replace (the LV tier is new).
 * The idle drain is roughly one-fiftieth of a hit per second, so a saber left switched on empties
 * itself over an evening rather than in a minute.
 */
public enum SaberVoltage {
    LV(3, 1_000_000L, 10, 20_000L, 250L, 6),
    MV(5, 10_000_000L, 15, 50_000L, 1_000L, 12),
    HV(6, 1_000_000_000L, 20, 1_000_000L, 20_000L, 18),
    EV(7, 10_000_000_000L, 25, 10_000_000L, 200_000L, 24),
    IV(7, 1_000_000_000_000L, 30, 100_000_000L, 2_000_000L, 30);

    public final int slots;
    public final long baseCapacity;
    public final double baseDamage;
    public final long baseCostPerHit;
    public final long idleDrainPerSecond;
    /** The complexity budget its modules may add up to — the same per tier as the drills. */
    public final int complexity;

    SaberVoltage(int slots, long baseCapacity, double baseDamage, long baseCostPerHit, long idleDrainPerSecond,
                 int complexity) {
        this.complexity = complexity;
        this.slots = slots;
        this.baseCapacity = baseCapacity;
        this.baseDamage = baseDamage;
        this.baseCostPerHit = baseCostPerHit;
        this.idleDrainPerSecond = idleDrainPerSecond;
    }
}
