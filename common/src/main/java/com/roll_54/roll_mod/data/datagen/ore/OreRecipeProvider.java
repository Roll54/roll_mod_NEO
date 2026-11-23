package com.roll_54.roll_mod.data.datagen.ore;

import com.roll_54.roll_mod.registry.GeneratedOreRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.recipe.CauldronWashingRecipe;
import com.roll_54.roll_mod.util.RollCauldronInteractions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Generates the storage-block crafting recipes for every generated ore:
 * <ul>
 *     <li>Pack: 9x {@code raw_<ore>} in a 3x3 -> 1x {@code raw_<ore>_block}</li>
 *     <li>Unpack: 1x {@code raw_<ore>_block} -> 9x {@code raw_<ore>}</li>
 * </ul>
 * Both directions are emitted by {@link RecipeProvider#nineBlockStorageRecipes}.
 */
public class OreRecipeProvider extends RecipeProvider {

    public OreRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        com.roll_54.roll_mod.data.datagen.RollMIMaterialDatagen.buildRecipes(recipeOutput);
        // Resolve generated ore items/blocks by registry-name (same pattern as OreLootTableProvider).
        Map<String, Item> items = GeneratedOreRegistry.ITEMS.getEntries().stream()
                .collect(Collectors.toMap(entry -> entry.getId().getPath(), DeferredHolder::get));

        Map<String, Block> blocks = GeneratedOreRegistry.BLOCKS.getEntries().stream()
                .collect(Collectors.toMap(entry -> entry.getId().getPath(), DeferredHolder::get));

        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
            Item rawItem = items.get("raw_" + def.id());
            Block rawBlock = blocks.get("raw_" + def.id() + "_block");

            // Every ore registers both sides, but guard defensively.
            if (rawItem == null || rawBlock == null) {
                continue;
            }

            // Emits both the 3x3 pack recipe (raw_<ore>_block) and the shapeless
            // unpack recipe (raw_<ore>). Recipe ids are forced into the roll_mod
            // namespace via the custom-name overload: the default helper strips the
            // namespace and would land these in "minecraft", colliding with vanilla
            // recipes for ores whose names match vanilla materials (gold, coal, ...).
            String packedName = "roll_mod:raw_" + def.id() + "_block";
            String unpackedName = "roll_mod:raw_" + def.id();
            nineBlockStorageRecipes(
                    recipeOutput,
                    RecipeCategory.MISC, rawItem,
                    RecipeCategory.BUILDING_BLOCKS, rawBlock,
                    packedName, null,
                    unpackedName, null
            );
        }
        cauldronWashing(recipeOutput);
    }

    /**
     * Cauldron washing for every ore that has both sides: {@code impure_<ore>_dust -> <ore>_dust} and
     * {@code crushed_<ore>_ore -> purified_<ore>_ore}, each dropping stone or clay dust half the time. These were
     * hard-coded in {@code RollCauldronInteractions}; as recipes, datapacks and KubeJS can change them.
     */
    private static void cauldronWashing(RecipeOutput out) {
        List<Item> byproducts = List.of(RollCauldronInteractions.getDustItems());
        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
            washing(out, "impure_" + def.id() + "_dust", def.id() + "_dust", byproducts);
            washing(out, "crushed_" + def.id() + "_ore", "purified_" + def.id() + "_ore", byproducts);
        }
    }

    private static void washing(RecipeOutput out, String inputPath, String outputPath, List<Item> byproducts) {
        Optional<Item> input = BuiltInRegistries.ITEM.getOptional(RollMod.id(inputPath));
        Optional<Item> output = BuiltInRegistries.ITEM.getOptional(RollMod.id(outputPath));
        if (input.isEmpty() || output.isEmpty()) return;
        out.accept(RollMod.id("cauldron_washing/" + inputPath),
                new CauldronWashingRecipe(Ingredient.of(input.get()), new ItemStack(output.get()), byproducts,
                        CauldronWashingRecipe.DEFAULT_BYPRODUCT_CHANCE, true),
                null);
    }
}
