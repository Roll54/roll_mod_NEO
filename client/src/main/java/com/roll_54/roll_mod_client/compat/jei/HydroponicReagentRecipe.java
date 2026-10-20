package com.roll_54.roll_mod_client.compat.jei;

import com.roll_54.roll_mod.data.datamap.ModDataMaps;
import com.roll_54.roll_mod.hydroponics.HydroponicReagents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One JEI entry of the Hydroponic Garden Bed's reagent data maps: a single item or fluid and what it
 * feeds into the bed. Exactly one of {@code item} / {@code fluid} is set.
 *
 * <p>{@code value} is taken straight from the data map, so it keeps the data map's units: per item
 * for items, per bucket (1000 mB) for fluids. Water is not data-driven — every source fluid in
 * {@code c:water} is listed at 1000 mB per bucket, matching {@link HydroponicReagents#fluidValue}.
 */
public record HydroponicReagentRecipe(HydroponicReagents.Kind kind, @Nullable Item item, @Nullable Fluid fluid, int value) {

    public boolean isFluid() {
        return fluid != null;
    }

    /** Reads the four synced data maps plus the water tag. Must run after the client has the data maps. */
    public static List<HydroponicReagentRecipe> collect() {
        List<HydroponicReagentRecipe> recipes = new ArrayList<>();

        BuiltInRegistries.ITEM.getDataMap(ModDataMaps.ACIDITY_ITEM).forEach((key, value) ->
                BuiltInRegistries.ITEM.getOptional(key).ifPresent(item ->
                        recipes.add(new HydroponicReagentRecipe(HydroponicReagents.Kind.ACIDITY, item, null, value.value()))));
        BuiltInRegistries.FLUID.getDataMap(ModDataMaps.ACIDITY_FLUID).forEach((key, value) ->
                BuiltInRegistries.FLUID.getOptional(key).filter(HydroponicReagentRecipe::isListable).ifPresent(fluid ->
                        recipes.add(new HydroponicReagentRecipe(HydroponicReagents.Kind.ACIDITY, null, fluid, value.value()))));

        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (isListable(fluid) && fluid.defaultFluidState().is(Tags.Fluids.WATER)) {
                recipes.add(new HydroponicReagentRecipe(HydroponicReagents.Kind.WATER, null, fluid, 1000));
            }
        }

        BuiltInRegistries.ITEM.getDataMap(ModDataMaps.FERTILIZER_ITEM).forEach((key, value) ->
                BuiltInRegistries.ITEM.getOptional(key).ifPresent(item ->
                        recipes.add(new HydroponicReagentRecipe(HydroponicReagents.Kind.FERTILIZER, item, null, value.amount()))));
        BuiltInRegistries.FLUID.getDataMap(ModDataMaps.FERTILIZER_FLUID).forEach((key, value) ->
                BuiltInRegistries.FLUID.getOptional(key).filter(HydroponicReagentRecipe::isListable).ifPresent(fluid ->
                        recipes.add(new HydroponicReagentRecipe(HydroponicReagents.Kind.FERTILIZER, null, fluid, value.amount()))));

        // Zero entries are rejected by the bed, so they would only be noise here.
        recipes.removeIf(recipe -> recipe.value == 0);
        recipes.sort(Comparator
                .comparing(HydroponicReagentRecipe::kind)
                .thenComparing(recipe -> recipe.value < 0) // acids before bases
                .thenComparing(recipe -> -Math.abs(recipe.value)));
        return recipes;
    }

    /** Flowing variants share the source's data but are not something a player can hold. */
    private static boolean isListable(Fluid fluid) {
        return fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState());
    }
}
