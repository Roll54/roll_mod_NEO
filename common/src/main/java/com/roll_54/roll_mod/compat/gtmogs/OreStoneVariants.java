package com.roll_54.roll_mod.compat.gtmogs;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.StoneVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets a vein name an ore once and get it in whatever rock it lands in. When a vein puts one of roll_mod's
 * ores ({@code stone_hematite}, {@code deepslate_hematite}, ...) over stone, deepslate, netherrack or one of
 * the {@link StoneVariant} rocks, the ore of the same material in that rock goes in instead — granite
 * hematite in granite, basalt pyrite in basalt — if that material has it. Vein files stay as they are.
 * See {@code mixin/OreStoneVariantPlacementMixin}.
 */
public final class OreStoneVariants {

    /** Vanilla rocks that have an ore layer of their own. Planet layers are left as the vein says. */
    private static final Map<BlockSubLayer, String> LAYER_ROCKS = Map.of(
            BlockSubLayer.STONE, "stone",
            BlockSubLayer.DEEPSLATE, "deepslate",
            BlockSubLayer.NETHERRACK, "netherrack");

    /** Ore block → (rock it is placed over → the same material's ore in that rock). */
    private static volatile Map<Block, Map<Block, BlockState>> byOre;

    private OreStoneVariants() {}

    /** The ore to place over {@code current}: {@code ore} itself unless its material comes in that rock. */
    public static BlockState forRock(BlockState ore, BlockState current) {
        Map<Block, BlockState> byRock = table().get(ore.getBlock());
        if (byRock == null) return ore;
        BlockState swapped = byRock.get(current.getBlock());
        return swapped != null ? swapped : ore;
    }

    /**
     * The other way round, for rock blobs (vanilla's granite, diorite, andesite, tuff, blackstone ore
     * features) written over an ore after the vein: a chunk's blobs can reach into a neighbour whose veins
     * are already in. The ore in that rock if {@code current} is one of ours that comes in it, else null.
     */
    public static BlockState rockOverOre(BlockState rock, BlockState current) {
        Map<Block, BlockState> byRock = table().get(current.getBlock());
        return byRock == null ? null : byRock.get(rock.getBlock());
    }

    private static Map<Block, Map<Block, BlockState>> table() {
        Map<Block, Map<Block, BlockState>> table = byOre;
        if (table != null) return table;
        synchronized (OreStoneVariants.class) {
            if (byOre != null) return byOre;
            Map<Block, Map<Block, BlockState>> built = new HashMap<>();
            for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
                Map<Block, BlockState> byRock = new HashMap<>();
                List<Block> ores = new ArrayList<>();
                for (BlockSubLayer base : def.bases()) {
                    Block ore = block(RollMod.id(base.id() + "_" + def.id()));
                    if (ore == null) continue;
                    ores.add(ore);
                    String rock = LAYER_ROCKS.get(base);
                    if (rock != null) put(byRock, rock, ore);
                }
                for (StoneVariant variant : def.variants()) {
                    Block ore = block(RollMod.id(def.variantBlockId(variant)));
                    if (ore == null) continue;
                    ores.add(ore);
                    put(byRock, variant.id(), ore);
                }
                if (byRock.isEmpty()) continue;
                for (Block ore : ores) built.put(ore, byRock);
            }
            byOre = built;
            return built;
        }
    }

    private static void put(Map<Block, BlockState> byRock, String vanillaRock, Block ore) {
        Block rock = block(ResourceLocation.withDefaultNamespace(vanillaRock));
        if (rock != null) byRock.put(rock, ore.defaultBlockState());
    }

    private static Block block(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }
}
