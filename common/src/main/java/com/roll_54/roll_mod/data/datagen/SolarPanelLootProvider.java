package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.registry.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Block loot for the solar panels. Exposed as a reusable {@link BlockLootSubProvider} rather than a
 * standalone {@code LootTableProvider}: vanilla's {@code LootTableProvider.getName()} is final and
 * returns "Loot Tables", so a mod may register only one loot provider. This sub-provider is added as
 * an extra entry on the mod's single loot provider ({@link com.roll_54.roll_mod.data.datagen.ore.OreLootTableProvider}).
 */
public final class SolarPanelLootProvider {
    private SolarPanelLootProvider() {
    }

    public static class SolarPanelLoot extends BlockLootSubProvider {
        public SolarPanelLoot(HolderLookup.Provider provider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        }

        @Override
        protected void generate() {
            for (DeferredBlock<Block> panel : BlockRegistry.SOLAR_PANELS) {
                dropSelf(panel.get());
            }
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return BlockRegistry.SOLAR_PANELS.stream().map(DeferredBlock::get).collect(Collectors.toList());
        }
    }
}
