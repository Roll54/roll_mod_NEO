package com.roll_54.roll_mod.gtmogs.api.worldgen.generator.indicators;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.WorldGeneratorUtils;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreBlockPlacer;
import dev.latvian.mods.rhino.util.HideFromJS;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Generates "indicators": decorative blocks placed near an ore vein (e.g. on the surface above it),
 * hinting at the vein's presence.
 *
 * <p>
 * Mirrors {@link com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator}'s registry-based
 * codec dispatch, so addons may register their own indicator types via
 * {@link com.roll_54.roll_mod.gtmogs.api.addon.IGTAddon#registerIndicatorGenerators()}.
 */
public abstract class IndicatorGenerator {

    public static final Codec<MapCodec<? extends IndicatorGenerator>> REGISTRY_CODEC = ResourceLocation.CODEC
            .flatXmap(rl -> Optional.ofNullable(WorldGeneratorUtils.INDICATOR_GENERATORS.get(rl))
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "No IndicatorGenerator with id " + rl + " registered")),
                    obj -> Optional.ofNullable(WorldGeneratorUtils.INDICATOR_GENERATORS.inverse().get(obj))
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error(() -> "IndicatorGenerator " + obj + " not registered")));
    public static final Codec<IndicatorGenerator> DIRECT_CODEC = REGISTRY_CODEC.dispatchStable(
            IndicatorGenerator::codec, Function.identity());

    public IndicatorGenerator() {}

    /**
     * Generate a map of indicator placers (by block position).
     *
     * <p>
     * As with vein generators, this is NOT supposed to directly place any blocks; the returned
     * {@link OreBlockPlacer}s are invoked at a later time, when the chunk containing them is generated.
     *
     * @param origin The vein's center, used to locate where indicators should be scattered.
     */
    @HideFromJS
    public abstract Map<BlockPos, OreBlockPlacer> generate(WorldGenLevel level, RandomSource random,
                                                           OreVeinDefinition entry, BlockPos origin);

    public abstract MapCodec<? extends IndicatorGenerator> codec();
}
