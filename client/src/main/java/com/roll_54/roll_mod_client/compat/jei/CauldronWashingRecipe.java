package com.roll_54.roll_mod_client.compat.jei;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.util.RollCauldronInteractions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple recipe holder for cauldron washing interactions, shared by the JEI and EMI plugins.
 */
public final class CauldronWashingRecipe {
    private final ItemStack input;
    private final ItemStack output;
    private final float dustDropChance;

    public CauldronWashingRecipe(ItemStack input, ItemStack output, float dustDropChance) {
        this.input = input;
        this.output = output;
        this.dustDropChance = dustDropChance;
    }

    /** Every pair {@link RollCauldronInteractions} registers, generated the same way from OreDefinitions. */
    public static List<CauldronWashingRecipe> collect() {
        List<CauldronWashingRecipe> recipes = new ArrayList<>();
        for (var ore : OreDefinitions.ORE_DEFINITIONS) {
            String id = ore.id();
            // impure_<id>_dust -> <id>_dust
            add(recipes, "impure_" + id + "_dust", id + "_dust");
            // crushed_<id>_ore -> purified_<id>_ore
            add(recipes, "crushed_" + id + "_ore", "purified_" + id + "_ore");
        }
        return recipes;
    }

    private static void add(List<CauldronWashingRecipe> recipes, String inputPath, String outputPath) {
        var in = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(RollMod.MODID, inputPath));
        var out = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(RollMod.MODID, outputPath));
        if (in.isPresent() && out.isPresent()) {
            recipes.add(new CauldronWashingRecipe(new ItemStack(in.get()), new ItemStack(out.get()),
                    RollCauldronInteractions.DUST_DROP_CHANCE));
        }
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public float getDustDropChance() {
        return dustDropChance;
    }
}
