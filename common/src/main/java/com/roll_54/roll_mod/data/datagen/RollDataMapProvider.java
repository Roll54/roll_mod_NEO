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
import com.roll_54.roll_mod.data.datamap.NutrientValue;

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
        gatherCropNutrients();
    }

    /**
     * N/P/K for the MV/HV Crop Managers (crops wiki §11.7). Items are per item, fluids per bucket.
     * Only the example lives here; real nitrogen and potassium sources are added as they are chosen.
     */
    private void gatherCropNutrients() {
        this.builder(ModDataMaps.NPK_ITEM)
                .add(ResourceLocation.parse("minecraft:bone_meal"), new NutrientValue(0, 10, 0), false);
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
        // Acids (positive) and bases (negative), per item. A slot eats one item per tick, so items top
        // out at their own value per tick — pH 1/7 (500 per bed per tick) is meant to run on fluids.
        this.builder(ModDataMaps.ACIDITY_ITEM)
                .add(ResourceLocation.parse("roll_mod:sulfur_dust"), new AcidityValue(250), false)
                .add(ResourceLocation.parse("roll_mod:sulfur_berry"), new AcidityValue(50), false)
                .add(ResourceLocation.parse("roll_mod:calcium_oxide"), new AcidityValue(-500), false)
                .add(ResourceLocation.parse("roll_mod:calcium_hydroxide"), new AcidityValue(-400), false)
                .add(ResourceLocation.parse("roll_mod:potassium_dust"), new AcidityValue(-300), false)
                .add(ResourceLocation.parse("roll_mod:trona_dust"), new AcidityValue(-250), false)
                .add(ResourceLocation.parse("roll_mod:calcium_carbonate"), new AcidityValue(-150), false);

        // Per bucket. A strong acid or base fills the whole ±10000 reservoir with one bucket.
        this.builder(ModDataMaps.ACIDITY_FLUID)
                .add(ResourceLocation.parse("modern_industrialization:sulfuric_acid"), new AcidityValue(10000), false)
                .add(ResourceLocation.parse("modern_industrialization:hydrochloric_acid"), new AcidityValue(5000), false)
                .add(ResourceLocation.parse("modern_industrialization:sodium_hydroxide"), new AcidityValue(-10000), false);

        this.builder(ModDataMaps.FERTILIZER_ITEM)
                .add(ResourceLocation.parse("minecraft:bone_meal"), new FertilizerValue(100), false)
                .add(ResourceLocation.parse("roll_mod:saltpeter_dust"), new FertilizerValue(200), false)
                .add(ResourceLocation.parse("roll_mod:apatite_dust"), new FertilizerValue(200), false)
                .add(ResourceLocation.parse("roll_mod:ammonium_chloride_dust"), new FertilizerValue(150), false)
                .add(ResourceLocation.parse("minecraft:rotten_flesh"), new FertilizerValue(25), false);

        this.builder(ModDataMaps.FERTILIZER_FLUID)
                .add(ResourceLocation.parse("extended_industrialization:npk_fertilizer"), new FertilizerValue(10000), false)
                .add(ResourceLocation.parse("extended_industrialization:composted_manure"), new FertilizerValue(2500), false)
                .add(ResourceLocation.parse("extended_industrialization:manure"), new FertilizerValue(1000), false);
    }
}
