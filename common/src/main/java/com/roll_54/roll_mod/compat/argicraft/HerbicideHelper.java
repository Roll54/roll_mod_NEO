package com.roll_54.roll_mod.compat.argicraft;

import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class HerbicideHelper {

    /** Lowest amount of biomass a removed weed can yield (at its earliest growth stage). */
    private static final int MIN_BIOMASS = 1;
    /** Highest amount of biomass a removed weed can yield (when fully grown). */
    private static final int MAX_BIOMASS = 6;

    private HerbicideHelper() {
    }

    /**
     * Maps a weed's growth stage onto the biomass yield it produces when a player removes it.
     *
     * <p>The amount scales linearly with how far the weed has grown, from {@value #MIN_BIOMASS} at the
     * earliest stage up to {@value #MAX_BIOMASS} when fully grown ({@link AgriGrowthStage#growthPercentage()}
     * returns a value in {@code (0, 1]}).
     */
    public static int biomassForStage(AgriGrowthStage stage) {
        if (stage == null) {
            return MIN_BIOMASS;
        }
        int amount = (int) Math.ceil(stage.growthPercentage() * MAX_BIOMASS);
        return Math.max(MIN_BIOMASS, Math.min(MAX_BIOMASS, amount));
    }

    /**
     * Spawns biomass at {@code pos} scaled by how grown the removed weed was. Server-side only; a
     * {@code null} stage or client level is a no-op.
     */
    public static void dropBiomass(Level level, BlockPos pos, AgriGrowthStage stage) {
        if (level == null || level.isClientSide || stage == null) {
            return;
        }
        ItemStack biomass = new ItemStack(ItemRegistry.BIOMASS.get(), biomassForStage(stage));
        CropBlock.spawnItem(level, pos, biomass);
    }

    /**
     * @return {@code true} if the crop's plant declares {@code weeds_resistance} in its datapack JSON,
     * meaning weeds never spawn on it and herbicide should not be applied by the managers.
     */
    public static boolean isWeedResistant(AgriCrop crop) {
        if (crop == null) {
            return false;
        }
        AgriPlant plant = crop.getPlant();
        return plant instanceof WeedResistant resistant && resistant.roll_mod$isWeedsResistant();
    }

    public static boolean applyHerbicide(ItemStack stack, CropBlockEntity crop) {
        if (!(crop instanceof AgriHerbicide herbicide)) {
            return false;
        }
        int amount = stack.getOrDefault(ComponentsRegistry.HERBICIDE.get(), 1);
        if (amount <= 0) {
            return false;
        }
        herbicide.roll_mod$addHerbicide(amount);
        return true;
    }
}

