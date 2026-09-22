package com.roll_54.roll_mod.data.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import com.roll_54.roll_mod.data.datamap.AcidityValue;
import com.roll_54.roll_mod.data.datamap.FertilizerValue;
import com.roll_54.roll_mod.data.datamap.ModDataMaps;

import java.util.concurrent.CompletableFuture;

public class RollDataMapProvider extends DataMapProvider {
    protected RollDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider){

        this.builder(NeoForgeDataMaps.COMPOSTABLES)
                .add(ResourceLocation.parse("silentgear:netherwood_leaves"), new Compostable(0.25f), false);
        this.builder(NeoForgeDataMaps.FURNACE_FUELS) // роллятина з милого щось знав...
                .add(ResourceLocation.parse("roll_mod:lignite_coal_dust"), new FurnaceFuel(1600), false);

        gatherHydroponicReagents();
    }

    /**
     * Reagents for the Hydroponic Garden Bed. See {@code docs/hydroponic-data-maps.md}.
     *
     * <p>{@code roll_mod:acidity} is signed: a positive value is an acid and feeds pH 1-3 operation,
     * a negative value is a base and feeds pH 5-7. {@code roll_mod:fertilizer} is millibuckets of
     * fertilizer. For the FLUID variants both values are <b>per bucket</b> (1000 mB), scaled
     * linearly, not per millibucket.
     *
     * <p>Only examples live here — real materials are meant to be added as they are introduced.
     * A bare number is shorthand for the object form, so {@code "roll_mod:sulfur_dust": 250} and
     * {@code "roll_mod:sulfur_dust": {"value": 250}} mean the same thing.
     */
    private void gatherHydroponicReagents() {
        this.builder(ModDataMaps.ACIDITY_ITEM)
                .add(ResourceLocation.parse("roll_mod:sulfur_dust"), new AcidityValue(250), false);

        this.builder(ModDataMaps.ACIDITY_FLUID)
                 .add(ResourceLocation.parse("modern_industrialization:sodium_hydroxide"), new AcidityValue(-1000), false);

        this.builder(ModDataMaps.FERTILIZER_ITEM)
                .add(ResourceLocation.parse("minecraft:bone_meal"), new FertilizerValue(100), false);

        this.builder(ModDataMaps.FERTILIZER_FLUID)
                .add(ResourceLocation.parse("extended_industrialization:npk_fertilizer"), new FertilizerValue(10000), false);

        this.builder(ModDataMaps.FERTILIZER_FLUID)
                .add(ResourceLocation.parse("extended_industrialization:composted_manure"), new FertilizerValue(2500), false);

        this.builder(ModDataMaps.FERTILIZER_FLUID)
                .add(ResourceLocation.parse("extended_industrialization:manure"), new FertilizerValue(1000), false);
    }
}
