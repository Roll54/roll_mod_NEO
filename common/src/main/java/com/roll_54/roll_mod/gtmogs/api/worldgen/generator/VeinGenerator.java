package com.roll_54.roll_mod.gtmogs.api.worldgen.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.WorldGeneratorUtils;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreBlockPlacer;
import dev.latvian.mods.rhino.util.HideFromJS;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class VeinGenerator {

    public static final Codec<MapCodec<? extends VeinGenerator>> REGISTRY_CODEC = ResourceLocation.CODEC
            .flatXmap(rl -> Optional.ofNullable(WorldGeneratorUtils.VEIN_GENERATORS.get(rl))
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "No VeinGenerator with id " + rl + " registered")),
                    obj -> Optional.ofNullable(WorldGeneratorUtils.VEIN_GENERATORS.inverse().get(obj))
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error(() -> "VeinGenerator " + obj + " not registered")));
    public static final Codec<VeinGenerator> DIRECT_CODEC = REGISTRY_CODEC.dispatchStable(VeinGenerator::codec,
            Function.identity());

    public VeinGenerator() {}

    /**
     * @return List of [block|material, chance]
     */
    public abstract List<VeinEntry> getAllEntries();

    public List<BlockState> getAllBlocks() {
        return getAllEntries().stream().map(entry -> entry.vein).toList();
    }

    public IntList getAllChances() {
        return IntArrayList.toList(getAllEntries().stream().mapToInt(VeinEntry::chance));
    }

    /**
     * Generate a map of all ore placers (by block position), for each block in this ore vein.
     *
     * <p>
     * Note that, if in any way possible, this is NOT supposed to directly place any of the vein's blocks, as their
     * respective ore placers are invoked at a later time, when the chunk containing them is actually generated.
     */
    @HideFromJS
    public abstract Map<BlockPos, OreBlockPlacer> generate(WorldGenLevel level, RandomSource random,
                                                           OreVeinDefinition entry, BlockPos origin);

    @HideFromJS
    public abstract VeinGenerator build();

    public abstract VeinGenerator copy();

    public abstract MapCodec<? extends VeinGenerator> codec();

    public record VeinEntry(BlockState vein, int chance) {

        public static VeinEntry ofBlock(BlockState state, int chance) {
            return new VeinEntry(state, chance);
        }
    }

    public static Stream<BlockState> mapTarget(List<OreConfiguration.TargetBlockState> target) {
        return target.stream().map(tbs -> tbs.state);
    }

    public static Stream<VeinEntry> mapTarget(List<OreConfiguration.TargetBlockState> target,
                                              int weight) {
        return mapTarget(target).map(entry -> new VeinEntry(entry, weight));
    }
}
