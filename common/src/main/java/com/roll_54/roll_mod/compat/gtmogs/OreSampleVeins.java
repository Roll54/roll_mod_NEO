package com.roll_54.roll_mod.compat.gtmogs;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.registry.GeneratedOreRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which ore a surface sample stands for, so a sample is only placed with its ore beneath it
 * ({@code SurfaceIndicatorGenerator.placeIndicator}). A {@code <base>_<ore>_ore_sample} stands for that ore in any
 * base or rock variant — a stone hematite sample over deepslate or granite hematite counts.
 */
public final class OreSampleVeins {

    /** How far down a sample looks for its ore: the whole height of an overworld-sized world. */
    private static final int DEPTH = 384;

    private static Map<Block, Set<Block>> oresBySample;

    private OreSampleVeins() {}

    /** True when {@code sample} is not one of roll_mod's samples, or its ore is somewhere straight under {@code pos}. */
    public static boolean hasOreBelow(BulkSectionAccess access, BlockPos pos, BlockState sample) {
        Set<Block> ores = ores().get(sample.getBlock());
        if (ores == null) return true;
        BlockPos.MutableBlockPos below = pos.mutable();
        for (int i = 0; i < DEPTH; i++) {
            below.move(0, -1, 0);
            if (ores.contains(access.getBlockState(below).getBlock())) return true;
        }
        return false;
    }

    private static synchronized Map<Block, Set<Block>> ores() {
        if (oresBySample != null) return oresBySample;
        Map<Block, Set<Block>> map = new HashMap<>();
        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
            Set<Block> ores = new HashSet<>();
            def.bases().forEach(base -> BuiltInRegistries.BLOCK
                    .getOptional(RollMod.id(base.id() + "_" + def.id())).ifPresent(ores::add));
            def.variants().forEach(variant -> BuiltInRegistries.BLOCK
                    .getOptional(RollMod.id(def.variantBlockId(variant))).ifPresent(ores::add));
            def.bases().forEach(base -> BuiltInRegistries.BLOCK
                    .getOptional(RollMod.id(GeneratedOreRegistry.oreSampleId(base, def.id())))
                    .ifPresent(sample -> map.put(sample, ores)));
        }
        oresBySample = map;
        return map;
    }
}
