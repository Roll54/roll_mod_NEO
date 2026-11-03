package com.roll_54.roll_mod.compat.gtmogs;

import com.quantumgarbage.gtmogs.api.worldgen.OreVeinDefinition;
import com.quantumgarbage.gtmogs.api.worldgen.generator.indicators.IndicatorGenerator;
import com.quantumgarbage.gtmogs.api.worldgen.generator.indicators.SurfaceIndicatorGenerator;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.StoneVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a vein's recipe page offers besides the ores it places: the raw ore those ores drop and the same
 * ores in the other rocks ({@link OreStoneVariants} may place them instead), so looking up
 * {@code raw_hematite} or {@code granite_hematite} finds the veins; and the vein's surface samples.
 */
public final class OreVeinLookups {

    /** Any of a material's ore blocks (bases and rock variants) → raw ore first, then all those blocks. */
    private static Map<Item, List<Item>> relatedByOre;

    private OreVeinLookups() {}

    /** Raw ores and rock variants of the roll_mod ores among {@code ores}, without the ores themselves. */
    public static List<Item> related(List<ItemStack> ores) {
        Set<Item> present = new LinkedHashSet<>();
        for (ItemStack stack : ores) present.add(stack.getItem());
        Set<Item> extra = new LinkedHashSet<>();
        for (Item ore : present) {
            List<Item> related = table().get(ore);
            if (related != null) extra.addAll(related);
        }
        extra.removeAll(present);
        return new ArrayList<>(extra);
    }

    /** The blocks a vein's surface indicators scatter, in order, once each. */
    public static List<ItemStack> indicators(OreVeinDefinition vein) {
        Set<Item> items = new LinkedHashSet<>();
        for (IndicatorGenerator generator : vein.indicatorGenerators()) {
            if (!(generator instanceof SurfaceIndicatorGenerator surface) || surface.blocks() == null) continue;
            for (Block block : surface.blocks()) {
                Item item = block.asItem();
                if (item != Items.AIR) items.add(item);
            }
        }
        return items.stream().map(ItemStack::new).toList();
    }

    private static synchronized Map<Item, List<Item>> table() {
        if (relatedByOre != null) return relatedByOre;
        Map<Item, List<Item>> map = new HashMap<>();
        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
            List<Item> related = new ArrayList<>();
            item("raw_" + def.id()).ifPresent(related::add);
            List<Item> ores = new ArrayList<>();
            for (BlockSubLayer base : def.bases()) item(base.id() + "_" + def.id()).ifPresent(ores::add);
            for (StoneVariant variant : def.variants()) item(def.variantBlockId(variant)).ifPresent(ores::add);
            related.addAll(ores);
            List<Item> frozen = List.copyOf(related);
            for (Item ore : ores) map.put(ore, frozen);
        }
        relatedByOre = map;
        return map;
    }

    private static Optional<Item> item(String path) {
        return BuiltInRegistries.ITEM.getOptional(RollMod.id(path));
    }
}
