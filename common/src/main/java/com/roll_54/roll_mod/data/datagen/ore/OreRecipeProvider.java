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

import java.util.Map;
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
    }
}
