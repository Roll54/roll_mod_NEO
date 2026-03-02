package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.data.datagen.py.OreDefinition;
import com.roll_54.roll_mod.data.datagen.py.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.py.PyTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.init.GeneratedOreRegistry;
import com.roll_54.roll_mod.init.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
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
        private final HolderLookup.RegistryLookup<Enchantment> enchantments;

        protected OreBlockLoot(HolderLookup.Provider provider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
            this.enchantments = provider.lookupOrThrow(Registries.ENCHANTMENT);
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

                    // Determine Base Dust Item (stone_dust, etc.)
                    Item baseDustItem = getDustForBase(base);

                    if (block != null) {
                        LootTable.Builder builder;

                        if (rawItem != null) {
                            // Pool for Raw Item (1-5, Fortune, No Silk Touch)
                            LootPool.Builder rawPool = LootPool.lootPool()
                                    .setRolls(UniformGenerator.between(1.0F, 1.0F))
                                    .add(LootItem.lootTableItem(rawItem)
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F)))
                                            .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))
                                            .when(this.doesNotHaveSilkTouch())
                                    );

                            // Start builder with Silk Touch pool (drops block)
                            // If has silk touch -> drop block.
                            // If no silk touch -> nothing from this pool.
                            builder = LootTable.lootTable().withPool(
                                    LootPool.lootPool()
                                            .setRolls(UniformGenerator.between(1.0F, 1.0F))
                                            .add(LootItem.lootTableItem(block)
                                                    .when(this.hasSilkTouch())
                                            )
                            );

                            // Add Raw Item pool
                            builder.withPool(rawPool);

                        } else {
                            // If no raw item, simple drop self
                            builder = this.createSingleItemTable(block);
                        }

                        // Add Base Dust Drop if available
                       if (baseDustItem != null) {
                            LootPool.Builder dustPool = LootPool.lootPool()
                                    .setRolls(UniformGenerator.between(1.0F, 1.0F)) // One roll of the pool
                                    .add(LootItem.lootTableItem(baseDustItem)
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))) // 1 to 5 items
                                            .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE))) // Multiplied by fortune
                                    )
                                    .when(this.doesNotHaveSilkTouch()); // Only if no silk touch

                            builder.withPool(dustPool);
                        }

                        this.add(block, builder);
                    }
                }
            }
        }

        private Item getDustForBase(BlockSubLayer base) {
             switch(base) {
                 case STONE: return ItemRegistry.STONE_DUST.get();
                 case DEEPSLATE: return ItemRegistry.DEEPSLATE_DUST.get();
                 case NETHERRACK: return ItemRegistry.NETHERRACK_DUST.get();
                 case END: return ItemRegistry.END_STONE_DUST.get();
                 case MOON: return ItemRegistry.MOON_STONE_DUST.get();
                 case MARS: return ItemRegistry.MARS_STONE_DUST.get();
                 case VENUS: return ItemRegistry.VENUS_STONE_DUST.get();
                 case MERCURY: return ItemRegistry.MERCURY_STONE_DUST.get();
                 default: return null;
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
