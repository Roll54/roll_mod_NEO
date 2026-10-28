package com.roll_54.roll_mod.compat.MBD2.greenhouse;

/** Balance knobs of the EV Greenhouse, in one place. */
public final class GreenhouseSettings {

    /**
     * EU/t a growing plant draws at the base speed, when every stage takes AgriCraft's expected time
     * (see {@link GreenhouseGrowth}). Close to what one Hydroponic Garden Bed costs (2500 EU/t).
     */
    public static final long RECIPE_EU = 2048;
    /**
     * The most EU/t the machine may draw before upgrades: the EV cable. Above {@link #RECIPE_EU} the
     * extra goes into speed, ramping up the way MI machines overclock.
     */
    public static final long BASE_MAX_EU = 8192;

    /** Water each growth stage takes, in mB, paid when the stage starts. */
    public static final int WATER_PER_STAGE = 1000;
    /**
     * Fertilizer each growth stage takes, in the units of the {@code roll_mod:fertilizer} data map
     * (EI's NPK fertilizer is 10000 per bucket, so 100 mB of it).
     */
    public static final int FERTILIZER_PER_STAGE = 1000;

    private GreenhouseSettings() {}
}
