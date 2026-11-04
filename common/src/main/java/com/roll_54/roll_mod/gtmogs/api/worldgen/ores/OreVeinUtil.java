package com.roll_54.roll_mod.gtmogs.api.worldgen.ores;

import com.roll_54.roll_mod.gtmogs.api.worldgen.IWorldGenLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;

import java.util.Optional;
import java.util.function.Function;

public class OreVeinUtil {

    private OreVeinUtil() {}

    public static boolean canPlaceOre(BlockState pState, Function<BlockPos, BlockState> pAdjacentStateAccessor,
                                      RandomSource pRandom, OreVeinDefinition entry,
                                      OreConfiguration.TargetBlockState pTargetState,
                                      BlockPos pMatablePos) {
        if (!pTargetState.target.test(pState, pRandom))
            return false;
        if (shouldSkipAirCheck(pRandom, entry.discardChanceOnAirExposure()))
            return true;

        return !Feature.isAdjacentToAir(pAdjacentStateAccessor, pMatablePos);
    }

    public static boolean canPlaceOre(BlockState pState, Function<BlockPos, BlockState> pAdjacentStateAccessor,
                                      RandomSource pRandom, OreVeinDefinition entry, IWorldGenLayer layer,
                                      BlockPos pMatablePos) {
        if (!layer.getTarget().test(pState, pRandom))
            return false;
        if (shouldSkipAirCheck(pRandom, entry.discardChanceOnAirExposure()))
            return true;

        return !Feature.isAdjacentToAir(pAdjacentStateAccessor, pMatablePos);
    }

    protected static boolean shouldSkipAirCheck(RandomSource pRandom, float pChance) {
        return pChance <= 0 || (!(pChance >= 1) && pRandom.nextFloat() >= pChance);
    }

    /**
     * Resolves a vein's center for the supplied chunk position.
     * 
     * <p>
     * Note that depending on the config value for the random vein offset, its actual
     * center may be outside the supplied chunk.
     * 
     * @return The origin of the vein to be generated.<br>
     *         {@code Optional.empty()} if no vein should exist for the specified chunk.
     */
    public static Optional<BlockPos> getVeinCenter(ChunkPos chunkPos, RandomSource random) {
        int randomOffset = ConfigHolder.INSTANCE.worldgen.oreVeins.oreVeinRandomOffset;

        if (!isVeinGridChunk(chunkPos))
            return Optional.empty();

        var chunkCenter = chunkPos.getMiddleBlockPosition(0);

        if (randomOffset == 0)
            return Optional.of(chunkCenter);

        return Optional.of(chunkCenter.offset(
                random.nextInt(-randomOffset, +randomOffset),
                0,
                random.nextInt(-randomOffset, +randomOffset)));
    }

    /**
     * @return The spacing (in chunks) between ore vein grid intersections.
     */
    static int getVeinGridSize() {
        return ConfigHolder.INSTANCE.worldgen.oreVeins.oreVeinGridSize;
    }

    /**
     * @return Whether the supplied chunk sits on an ore vein grid intersection, i.e. whether it can hold a vein
     *         center at all.
     *
     *         <p>
     *         This is the authoritative grid check, used both by {@link #getVeinCenter(ChunkPos, RandomSource)} and
     *         by the surrounding-chunk scan in {@code OreGenCache}, which relies on it to skip chunks that can never
     *         produce a vein. Keep the two callers sharing this method: if the scan were to accept a wider set of
     *         chunks than the generator, the difference would only cost performance, but a narrower set would
     *         silently drop veins.
     */
    public static boolean isVeinGridChunk(ChunkPos chunkPos) {
        int gridSize = getVeinGridSize();
        return chunkPos.x % gridSize == 0 && chunkPos.z % gridSize == 0;
    }

    /**
     * @return The radius (in chunks) to search for adjacent veins.<br>
     *         Depends on the largest registered vein size, as well as the configured random vein offset.
     */
    static int getMaxVeinSearchDistance() {
        double halfVeinSize = GTOreVeins.getLargestVeinSize() / 2.0;
        int randomOffset = ConfigHolder.INSTANCE.worldgen.oreVeins.oreVeinRandomOffset;

        return (int) Math.ceil((halfVeinSize + randomOffset) / 16.0);
    }

    /**
     * @return An upper bound on the number of origin chunks resolved for each generated chunk.
     *
     *         <p>
     *         Only grid intersections are visited (see {@link #isVeinGridChunk}), so this counts those rather than
     *         the full area of the search window -- at a grid size of 3 the two differ by roughly 9x.
     */
    static int getOriginChunksPerGeneratedChunk() {
        int perAxis = (2 * getMaxVeinSearchDistance()) / getVeinGridSize() + 1;
        return perAxis * perAxis;
    }
}
