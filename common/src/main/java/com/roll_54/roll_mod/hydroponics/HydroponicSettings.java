package com.roll_54.roll_mod.hydroponics;

import com.agricraft.agricraft.api.codecs.AgriSoilCondition;
import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

/**
 * The per-node configuration of a Hydroponic Garden Bed: three slider values plus the on/off switch.
 *
 * <p>Each slider maps one-to-one onto an AgriCraft soil property, and each level carries the
 * per-tick, <b>per bed</b> cost of holding it:
 *
 * <pre>
 * pH        1..7  HIGHLY_ACIDIC .. HIGHLY_ALKALINE   acidity  -500 -100 -10   0  +10 +100 +500
 * water     1..6  ARID .. FLOODED                    water       0    2   3   4    5    6
 * fertility 1..6  NONE .. VERY_HIGH                  fertilizer  0    2   3   4    5    6
 * </pre>
 *
 * <p>The acidity figure is the signed delta applied to the node's acidity buffer: an acidic setting
 * eats stored acid (a positive buffer) and an alkaline one eats stored base (a negative buffer), so
 * running the bed always drags the buffer back toward zero.
 */
public record HydroponicSettings(int ph, int water, int fertility, boolean enabled) {

    public static final int PH_MIN = 1;
    public static final int PH_MAX = 7;
    public static final int WATER_MIN = 1;
    public static final int WATER_MAX = 6;
    public static final int FERTILITY_MIN = 1;
    public static final int FERTILITY_MAX = 6;

    /** Neutral, dry, barren and switched off — what a freshly placed lone bed starts with. */
    public static final HydroponicSettings DEFAULT = new HydroponicSettings(4, 1, 1, false);

    /** Signed acidity delta per tick per bed, indexed by {@code ph - 1}. */
    private static final int[] ACIDITY_DELTA = {-500, -100, -10, 0, 10, 100, 500};
    /** Water mB per tick per bed, indexed by {@code water - 1}. */
    private static final int[] WATER_COST = {0, 2, 3, 4, 5, 6};
    /** Fertilizer mB per tick per bed, indexed by {@code fertility - 1}. */
    private static final int[] FERTILIZER_COST = {0, 2, 3, 4, 5, 6};

    /** EU per tick per enabled bed. */
    public static final long ENERGY_COST = 2500L;

    /** Growth multiplier handed to AgriCraft while a bed is fully supplied. */
    public static final double GROWTH_MODIFIER = 6.0D;

    public HydroponicSettings {
        ph = Math.clamp(ph, PH_MIN, PH_MAX);
        water = Math.clamp(water, WATER_MIN, WATER_MAX);
        fertility = Math.clamp(fertility, FERTILITY_MIN, FERTILITY_MAX);
    }

    public HydroponicSettings withPh(int value) {
        return new HydroponicSettings(value, water, fertility, enabled);
    }

    public HydroponicSettings withWater(int value) {
        return new HydroponicSettings(ph, value, fertility, enabled);
    }

    public HydroponicSettings withFertility(int value) {
        return new HydroponicSettings(ph, water, value, enabled);
    }

    public HydroponicSettings withEnabled(boolean value) {
        return new HydroponicSettings(ph, water, fertility, value);
    }

    public AgriSoilCondition.Acidity acidity() {
        return AgriSoilCondition.Acidity.values()[ph - 1];
    }

    public AgriSoilCondition.Humidity humidity() {
        return AgriSoilCondition.Humidity.values()[water - 1];
    }

    public AgriSoilCondition.Nutrients nutrients() {
        return AgriSoilCondition.Nutrients.values()[fertility - 1];
    }

    public int acidityDelta() {
        return ACIDITY_DELTA[ph - 1];
    }

    public int waterCost() {
        return WATER_COST[water - 1];
    }

    public int fertilizerCost() {
        return FERTILIZER_COST[fertility - 1];
    }

    // AgriCraft already names every one of these levels, in every language it ships; reusing its
    // keys keeps the bed's vocabulary identical to the crop requirement pages players read.

    public static String acidityKey(int ph) {
        return "agricraft.soil.acidity."
                + AgriSoilCondition.Acidity.values()[Math.clamp(ph, PH_MIN, PH_MAX) - 1].name().toLowerCase(Locale.ROOT);
    }

    public static String humidityKey(int water) {
        return "agricraft.soil.humidity."
                + AgriSoilCondition.Humidity.values()[Math.clamp(water, WATER_MIN, WATER_MAX) - 1].name().toLowerCase(Locale.ROOT);
    }

    public static String nutrientsKey(int fertility) {
        return "agricraft.soil.nutrients."
                + AgriSoilCondition.Nutrients.values()[Math.clamp(fertility, FERTILITY_MIN, FERTILITY_MAX) - 1].name().toLowerCase(Locale.ROOT);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("ph", (byte) ph);
        tag.putByte("water", (byte) water);
        tag.putByte("fertility", (byte) fertility);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    public static HydroponicSettings load(CompoundTag tag) {
        if (tag.isEmpty()) {
            return DEFAULT;
        }
        return new HydroponicSettings(
                tag.getByte("ph"),
                tag.getByte("water"),
                tag.getByte("fertility"),
                tag.getBoolean("enabled"));
    }
}
