package com.roll_54.roll_mod_client.compat.jei;

import com.roll_54.roll_mod.compat.MBD2.machine.RollMBD2Machines.CropManagerTier;
import com.roll_54.roll_mod.data.datamap.ModDataMaps;
import com.roll_54.roll_mod.data.datamap.NutrientValue;
import com.roll_54.roll_mod.registry.TagRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One recipe-viewer entry of the LV/MV/HV Crop Managers' inputs: a single item or fluid, the slot or
 * tank it goes into, and the lowest tier that has that slot. Exactly one of {@code item} / {@code fluid}
 * is set. Built from the same tags and {@code roll_mod:npk} data maps the machines filter by
 * (see {@code RollMBD2Machines#cropManagerSettings}), so datapack changes show up after a reload.
 */
public record CropManagerInputRecipe(Input input, @Nullable Item item, @Nullable Fluid fluid,
                                     @Nullable NutrientValue npk) {

    /** The machine's inputs, in GUI order. */
    public enum Input {
        HERBICIDE_SLOT(CropManagerTier.LV),
        HERBICIDE_TANK(CropManagerTier.LV),
        NUTRIENT_SLOT(CropManagerTier.MV),
        NUTRIENT_TANK(CropManagerTier.MV),
        MUTAGEN_TANK(CropManagerTier.HV);

        public final CropManagerTier minTier;

        Input(CropManagerTier minTier) {
            this.minTier = minTier;
        }

        public String key() {
            return "jei.roll_mod.crop_manager_inputs." + name().toLowerCase(Locale.ROOT);
        }

        /** "LV+", "MV+" or "HV". */
        public String tiers() {
            return minTier == CropManagerTier.HV ? "HV" : minTier.name() + "+";
        }
    }

    public boolean isFluid() {
        return fluid != null;
    }

    public static List<CropManagerInputRecipe> collect() {
        List<CropManagerInputRecipe> recipes = new ArrayList<>();
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TagRegistry.HERBICIDES)) {
            recipes.add(new CropManagerInputRecipe(Input.HERBICIDE_SLOT, item.value(), null, null));
        }
        for (Holder<Fluid> fluid : BuiltInRegistries.FLUID.getTagOrEmpty(TagRegistry.HERBICIDE_FLUIDS)) {
            if (isListable(fluid.value())) {
                recipes.add(new CropManagerInputRecipe(Input.HERBICIDE_TANK, null, fluid.value(), null));
            }
        }
        BuiltInRegistries.ITEM.getDataMap(ModDataMaps.NPK_ITEM).forEach((key, value) ->
                BuiltInRegistries.ITEM.getOptional(key).ifPresent(item ->
                        recipes.add(new CropManagerInputRecipe(Input.NUTRIENT_SLOT, item, null, value))));
        BuiltInRegistries.FLUID.getDataMap(ModDataMaps.NPK_FLUID).forEach((key, value) ->
                BuiltInRegistries.FLUID.getOptional(key).filter(CropManagerInputRecipe::isListable).ifPresent(fluid ->
                        recipes.add(new CropManagerInputRecipe(Input.NUTRIENT_TANK, null, fluid, value))));
        for (Holder<Fluid> fluid : BuiltInRegistries.FLUID.getTagOrEmpty(TagRegistry.MUTAGEN_FLUIDS)) {
            if (isListable(fluid.value())) {
                recipes.add(new CropManagerInputRecipe(Input.MUTAGEN_TANK, null, fluid.value(), null));
            }
        }
        return recipes;
    }

    /** The fluid's bucket, or empty when it has none worth showing. */
    public ItemStack bucket() {
        if (fluid == null) return ItemStack.EMPTY;
        ItemStack bucket = new ItemStack(fluid.getBucket());
        return bucket.is(Items.BUCKET) ? ItemStack.EMPTY : bucket;
    }

    /** Every tier that has this input, for catalysts / workstations. */
    public static List<ItemStack> machines(Input input) {
        List<ItemStack> stacks = new ArrayList<>();
        for (CropManagerTier tier : CropManagerTier.values()) {
            if (tier.ordinal() >= input.minTier.ordinal()) stacks.add(machine(tier));
        }
        return stacks;
    }

    public static ItemStack machine(CropManagerTier tier) {
        // MBD2 registers each machine's item under the machine's own id.
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(tier.id).orElse(Items.WHEAT_SEEDS));
    }

    private static boolean isListable(Fluid fluid) {
        return fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState());
    }
}
