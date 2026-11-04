package com.roll_54.roll_mod.compat.gtmogs;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.StoneVariant;
import com.roll_54.roll_mod.registry.GeneratedOreRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What one slot of a vein's recipe page (one layer of the vein) cycles through: for each of roll_mod's ores in
 * that layer, the material's surface sample, the ore in every rock it can be in within the vein's worlds
 * ({@link OreStoneVariants} may place granite or tuff ores instead), and the raw ore. Everything a page cycles
 * through is also what looks the vein up, so {@code raw_hematite}, {@code granite_hematite} and a hematite
 * sample all find its veins.
 */
public final class OreVeinLookups {

    /** Any of roll_mod's ore blocks (bases and rock variants) → its material. */
    private static Map<Block, OreDefinition> materialByOre;

    private OreVeinLookups() {}

    /**
     * The stacks one vein layer's slot cycles through, in order: per block, sample(s), ores, raw ore for roll_mod's
     * ores (limited to the rocks of {@code worlds}), or just the block's item for anything else. No duplicates.
     */
    public static List<ItemStack> slotStacks(List<BlockState> blocks, Set<ResourceKey<Level>> worlds) {
        Set<Item> items = new LinkedHashSet<>();
        for (BlockState state : blocks) {
            Block block = state.getBlock();
            OreDefinition def = table().get(block);
            if (def == null) {
                add(items, block.asItem());
                continue;
            }
            List<BlockSubLayer> bases = def.bases().stream()
                    .filter(base -> OreMaterials.runsIn(base, worlds))
                    .toList();
            for (BlockSubLayer base : bases) {
                item(GeneratedOreRegistry.oreSampleId(base, def.id())).ifPresent(items::add);
            }
            for (BlockSubLayer base : bases) {
                item(base.id() + "_" + def.id()).ifPresent(items::add);
                for (StoneVariant variant : def.variants()) {
                    if (variant.parent() == base) item(def.variantBlockId(variant)).ifPresent(items::add);
                }
            }
            // The vein's own block even if it is in a rock none of its worlds has.
            add(items, block.asItem());
            item("raw_" + def.id()).ifPresent(items::add);
        }
        return items.stream().map(ItemStack::new).toList();
    }

    /** The material of one of roll_mod's ore blocks (any base or rock variant), or null for anything else. */
    public static OreDefinition materialOf(Block block) {
        return table().get(block);
    }

    private static void add(Set<Item> items, Item item) {
        if (item != Items.AIR) items.add(item);
    }

    private static synchronized Map<Block, OreDefinition> table() {
        if (materialByOre != null) return materialByOre;
        Map<Block, OreDefinition> map = new HashMap<>();
        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
            for (BlockSubLayer base : def.bases()) block(base.id() + "_" + def.id()).ifPresent(b -> map.put(b, def));
            for (StoneVariant variant : def.variants()) block(def.variantBlockId(variant)).ifPresent(b -> map.put(b, def));
        }
        materialByOre = map;
        return map;
    }

    private static Optional<Item> item(String path) {
        return BuiltInRegistries.ITEM.getOptional(RollMod.id(path));
    }

    private static Optional<Block> block(String path) {
        return BuiltInRegistries.BLOCK.getOptional(RollMod.id(path));
    }
}
