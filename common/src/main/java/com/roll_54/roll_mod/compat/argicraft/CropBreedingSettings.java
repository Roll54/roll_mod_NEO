package com.roll_54.roll_mod.compat.argicraft;

/**
 * Every number behind gene ceilings, breeding gates, nutrients and mutagen, in one place. The rules
 * themselves live in {@link CropBreeding}; see {@code docs/crops-wiki.md} §11.7.
 */
public final class CropBreedingSettings {

    /**
     * Stats are stored up to this. AgriCraft's own {@code <stat>_max} config is overridden
     * ({@code AgriStatMaxMixin}) because its allele cache is sized from the max at startup.
     */
    public static final int STAT_MAX = 20;
    /**
     * What AgriCraft's formulas treat as "the maximum": values 1–10 keep their old meaning (resistance
     * 10 = no weeds, fertility 10 = always picked, mutativity 10 = always +1) and 11–20 extrapolate.
     */
    public static final int STAT_REFERENCE_MAX = 10;

    /** Ceiling a bred allele may climb to with no help. */
    public static final int CEILING_DEFAULT = 5;
    /** Both parents with at least {@link #HERBICIDE_GATE} herbicide. */
    public static final int CEILING_HERBICIDE = 10;
    /** ...and both supplied with N, P and K. */
    public static final int CEILING_NUTRIENTS = 15;
    /** ...and both with at least {@link #MUTAGEN_GATE} mutagen, same species. */
    public static final int CEILING_MUTAGEN = 20;
    /** Genes above this need N/P/K upkeep and fall back to it on an unsupplied harvest. */
    public static final int CEILING_UNSUPPLIED = CEILING_HERBICIDE;

    public static final int HERBICIDE_GATE = 1000;
    /** Also the species-mutation gate: both parents need this much. */
    public static final int MUTAGEN_GATE = 1000;

    /** Mutagen lost per second, worked out lazily from game time (crops don't tick). */
    public static final int MUTAGEN_BLEED_PER_SECOND = 100;
    /** Per Mutagen Syringe. */
    public static final int MUTAGEN_PER_SYRINGE = 500;
    /** The HV Crop Manager tops crops up to this; hand application isn't capped. */
    public static final int MUTAGEN_MANAGER_CAP = 4000;

    /** N, P and K each paid on every harvest; also the "supplied" threshold. */
    public static final int NUTRIENT_COST_PER_HARVEST = 10;
    /** Crop Managers top each crop's N, P and K pools up to this. */
    public static final int NUTRIENT_CROP_CAP = 100;
    /** A Crop Manager's own N, P and K buffers. */
    public static final int NUTRIENT_MANAGER_BUFFER = 1000;
    /** Growth-rate multiplier while supplied; not applied on a Hydroponic Garden Bed. */
    public static final double NUTRIENT_GROWTH_MULTIPLIER = 2.0;

    /** Herbicide per millibucket of each fluid tier: 1000 mB equals one item of that tier. */
    public static final double HERBICIDE_FLUID_TIER_1_PER_MB = 0.2;
    public static final double HERBICIDE_FLUID_TIER_2_PER_MB = 1.0;
    public static final double HERBICIDE_FLUID_TIER_3_PER_MB = 4.0;

    private CropBreedingSettings() {
    }
}
