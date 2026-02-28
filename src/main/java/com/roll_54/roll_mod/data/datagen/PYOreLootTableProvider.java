package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.PYDatagen.PYOreDataGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

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
            Map<String, Item> rawItems = PYOreDataGen.ORE_ITEMS.getEntries().stream()
                    .filter(entry -> entry.getId().getPath().startsWith("raw_"))
                    .collect(Collectors.toMap(entry -> entry.getId().getPath(), entry -> entry.get()));

            for (var entry : PYOreDataGen.ORE_BLOCKS.getEntries()) {
                Block block = entry.get();
                String path = entry.getId().getPath();
                int idx = path.indexOf('_');
                String oreName = idx >= 0 && idx + 1 < path.length() ? path.substring(idx + 1) : path;
                Item raw = rawItems.get("raw_" + oreName);
                if (raw != null) {
                    add(block, b -> createOreDrop(b, raw));
                } else {
                    dropSelf(block);
                }
            }
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return (Iterable<Block>) PYOreDataGen.ORE_BLOCKS.getEntries().stream().map(holder -> holder.get()).toList();
        }
    }
}
