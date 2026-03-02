package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.data.datagen.py.OreDefinition;
import com.roll_54.roll_mod.data.datagen.py.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.py.PyTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.init.GeneratedOreRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PYOreLootTableProvider extends LootTableProvider {
    public PYOreLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Set.of(), List.of(new SubProviderEntry(OreBlockLoot::new, LootContextParamSets.BLOCK)), lookupProvider);
    }

    private static class OreBlockLoot extends BlockLootSubProvider {
        protected OreBlockLoot(HolderLookup.Provider provider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        }

        @Override
        protected void generate() {
            // Map items and blocks from GeneratedOreRegistry
            Map<String, Item> items = GeneratedOreRegistry.ITEMS.getEntries().stream()
                    .collect(Collectors.toMap(entry -> entry.getId().getPath(), DeferredHolder::get));

            Map<String, Block> blocks = GeneratedOreRegistry.BLOCKS.getEntries().stream()
                    .collect(Collectors.toMap(entry -> entry.getId().getPath(), DeferredHolder::get));

            for (OreDefinition def : OreDefinitions.ALL) {
                // Determine Raw Item
                String rawItemName = "raw_" + def.oreName();
                Item rawItem = items.get(rawItemName);

                // For each base block variant
                for (BlockSubLayer base : def.bases()) {
                    String blockName = base.id() + "_" + def.oreName();
                    Block block = blocks.get(blockName);

                    if (block != null) {
                        if (rawItem != null) {
                            // Silk touch -> Block, otherwise -> Item (with fortune)
                            this.add(block, (b) -> this.createOreDrop(b, rawItem));
                        } else {
                            // If no raw item, drop self
                            this.dropSelf(block);
                        }
                    }
                }
            }
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return GeneratedOreRegistry.BLOCKS.getEntries().stream()
                    .map(DeferredHolder::get)
                    .collect(Collectors.toList());
        }
    }
}
