package com.roll_54.roll_mod.compat.MBD2.greenhouse;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.genetic.AgriGenomeProviderItem;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.roll_54.roll_mod.hydroponics.HydroponicSettings;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * How the EV Greenhouse turns an AgriCraft plant into time and crops, with no crop in the world.
 * Shared by the machine ({@link GrowerTrait}) and the JEI yield page, so both always agree.
 *
 * <p>Growth follows AgriCraft's own crop on a fully supplied Hydroponic Garden Bed: a stage advances
 * on a random tick with chance {@code soil × (growth_chance + growth × growth_bonus × multiplier)}
 * (see docs/crops-wiki.md §2), the soil being the bed's {@link HydroponicSettings#GROWTH_MODIFIER}.
 * Instead of rolling, the greenhouse spends the <em>expected</em> time: a block gets a random tick
 * {@value #RANDOM_TICKS}/4096 of game ticks, so a stage takes {@code 4096 / (3 × min(1, chance))} ticks.
 * Weeds, light, seasons, soil and other requirements do not apply inside the greenhouse.
 *
 * <p>Harvest is AgriCraft's: {@code (gain + 3) / 3} rolls of the plant's products.
 */
public final class GreenhouseGrowth {

    /** Vanilla's default {@code randomTickSpeed}: random ticks per chunk section per game tick. */
    public static final int RANDOM_TICKS = 3;
    private static final double TICKS_PER_RANDOM_TICK = 4096.0 / RANDOM_TICKS;

    private GreenhouseGrowth() {}

    /**
     * The genome of a seed stack, or null if it has none. An AgriCraft seed carries its own genome;
     * AgriCraft's genome adapter only covers the other way in, a plain seed item (wheat seeds, ...)
     * that some plant lists as its seed, read as that plant at base stats.
     */
    @Nullable
    public static AgriGenome genome(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof AgriGenomeProviderItem provider) {
            return provider.getGenome(stack).orElse(null);
        }
        return AgriApi.getGenomeAdapter(stack).flatMap(adapter -> adapter.valueOf(stack)).orElse(null);
    }

    /** The plant a genome is of, or null when its species is unknown. */
    @Nullable
    public static AgriPlant plant(AgriGenome genome) {
        AgriPlant plant = AgriApi.getPlant(genome.getSpeciesGene().getTrait()).orElse(null);
        return plant == AgriPlant.NO_PLANT ? null : plant;
    }

    /** The chance per random tick that a stage advances, before capping at 1. */
    public static double growthChance(AgriPlant plant, AgriGrowthStage stage, int growth) {
        return HydroponicSettings.GROWTH_MODIFIER * (plant.getGrowthChance(stage)
                + growth * plant.getBonusGrowthChance(stage) * CoreConfig.growthMultiplier);
    }

    /** Expected game ticks for {@code stage} to advance once, or -1 if it never does. */
    public static long stageTicks(AgriPlant plant, AgriGrowthStage stage, int growth) {
        double chance = Math.min(1.0, growthChance(plant, stage, growth));
        if (chance <= 0) return -1;
        return (long) Math.ceil(TICKS_PER_RANDOM_TICK / chance);
    }

    /** Expected ticks from {@code from} to maturity, or -1 if some stage never advances. */
    public static long ticksToMature(AgriPlant plant, AgriGrowthStage from, int growth) {
        long total = 0;
        for (AgriGrowthStage stage = from; !stage.isMature(); stage = new AgriGrowthStage(stage.index() + 1, stage.total())) {
            long ticks = stageTicks(plant, stage, growth);
            if (ticks < 0) return -1;
            total += ticks;
        }
        return total;
    }

    /** One full regrowth: from the stage a harvest leaves the plant at back to maturity. */
    public static long cycleTicks(AgriPlant plant, int growth) {
        return ticksToMature(plant, plant.getGrowthStageAfterHarvest(), growth);
    }

    /** AgriCraft's harvest rolls: 1,1,2,2,2,3,3,3,4,4 for gain 1..10, on up to 7 at gain 20. */
    public static int harvestRolls(int gain) {
        return (3 + gain) / 3;
    }
}
