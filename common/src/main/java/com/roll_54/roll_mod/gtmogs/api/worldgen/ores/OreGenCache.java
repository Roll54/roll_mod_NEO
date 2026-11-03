package com.roll_54.roll_mod.gtmogs.api.worldgen.ores;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Used for caching ore veins between generated chunks.
 * 
 * <p>
 * Uses the {@link OreGenerator} to generate new veins in case no vein is cached for a queried chunk.
 */
public class OreGenCache {

    /**
     * Neighbouring chunks share the same surrounding vein origins, but during a pregen burst or a slow decoration
     * queue the gap between two chunks reaching for the same origin can comfortably exceed the previous 30s.
     */
    private static final long CACHE_TTL_SECONDS = 300;

    @Getter
    private final OreGenerator oreGenerator = new OreGenerator();

    private final int oreGenerationCacheSize = ConfigHolder.INSTANCE != null ?
            ConfigHolder.INSTANCE.worldgen.oreVeins.oreGenerationChunkCacheSize : 512;

    private final int oreIndicatorCacheSize = ConfigHolder.INSTANCE != null ?
            ConfigHolder.INSTANCE.worldgen.oreVeins.oreIndicatorChunkCacheSize : 2048;

    /**
     * The metadata cache must outlive the two caches that derive from it: evicting an entry here forces every
     * dependent vein and indicator entry to regenerate from scratch. Sized to cover both dependents.
     */
    private final int veinMetadataCacheSize = Math.max(oreGenerationCacheSize, oreIndicatorCacheSize);

    /**
     * Vein metadata is small and cheap to hold (a chunk pos, a block pos and a definition holder per vein), but
     * expensive to rebuild, since a miss cascades into regenerating vein shapes and indicators. Held with strong
     * references so the size bound is what actually governs it.
     */
    private final Cache<ChunkPos, List<GeneratedVeinMetadata>> veinMetadataByOrigin = CacheBuilder.newBuilder()
            .maximumSize(veinMetadataCacheSize)
            .expireAfterAccess(CACHE_TTL_SECONDS, TimeUnit.SECONDS)
            .build();

    /**
     * Deliberately still {@link CacheBuilder#softValues()}: a single large vein can hold hundreds of thousands of
     * {@link OreBlockPlacer}s, so the entry count alone is a poor proxy for heap use. Soft references keep a
     * pregen burst from exhausting the heap; the raised TTL is what reduces the thrash.
     */
    private final Cache<ChunkPos, List<GeneratedVein>> generatedVeinsByOrigin = CacheBuilder.newBuilder()
            .maximumSize(oreGenerationCacheSize)
            .expireAfterAccess(CACHE_TTL_SECONDS, TimeUnit.SECONDS)
            .softValues()
            .build();

    /** Same reasoning as {@link #generatedVeinsByOrigin}. */
    private final Cache<ChunkPos, List<GeneratedVein>> generatedIndicatorsByOrigin = CacheBuilder.newBuilder()
            .maximumSize(oreIndicatorCacheSize)
            .expireAfterAccess(CACHE_TTL_SECONDS, TimeUnit.SECONDS)
            .softValues()
            .build();

    private List<GeneratedVeinMetadata> getOrCreateVeinMetadata(WorldGenLevel level, ChunkGenerator generator,
                                                                ChunkPos chunkPos) {
        try {
            return veinMetadataByOrigin
                    .get(chunkPos, () -> oreGenerator.generateMetadata(level, generator, chunkPos));
        } catch (ExecutionException e) {
            GTMOGS.LOGGER.error("Cannot create vein position in chunk {}", chunkPos, e);
            return List.of();
        }
    }

    /**
     * Get (or create) all veins to be generated, surrounding the supplied chunk.
     *
     * <p>
     * The search radius depends on the largest registered vein size, as well as the relevant config options.
     */
    public List<GeneratedVein> consumeChunkVeins(WorldGenLevel level, ChunkGenerator generator, ChunkAccess chunk) {
        return getSurroundingChunks(chunk.getPos(), OreVeinUtil.getMaxVeinSearchDistance()).flatMap(chunkPos -> {
            try {
                return generatedVeinsByOrigin
                        .get(chunkPos,
                                () -> oreGenerator.generateOres(level,
                                        getOrCreateVeinMetadata(level, generator, chunkPos), chunkPos))
                        .stream();
            } catch (ExecutionException e) {
                GTMOGS.LOGGER.error("Cannot create vein in chunk {}", chunkPos, e);
                return Stream.empty();
            }
        }).filter(Objects::nonNull).toList();
    }

    /**
     * Get (or create) all indicators to be generated, surrounding the supplied chunk.
     *
     * <p>
     * Indicators are scattered within each vein's footprint, so the search radius matches the one used for veins.
     */
    public List<GeneratedVein> consumeChunkIndicators(WorldGenLevel level, ChunkGenerator generator,
                                                      ChunkAccess chunk) {
        return getSurroundingChunks(chunk.getPos(), OreVeinUtil.getMaxVeinSearchDistance()).flatMap(chunkPos -> {
            try {
                return generatedIndicatorsByOrigin
                        .get(chunkPos,
                                () -> oreGenerator.generateIndicators(level,
                                        getOrCreateVeinMetadata(level, generator, chunkPos), chunkPos))
                        .stream();
            } catch (ExecutionException e) {
                GTMOGS.LOGGER.error("Cannot create indicators in chunk {}", chunkPos, e);
                return Stream.empty();
            }
        }).filter(Objects::nonNull).toList();
    }

    /**
     * Enumerates the chunks around {@code center} that can actually hold a vein origin.
     *
     * <p>
     * Only chunks sitting on an ore vein grid intersection are visited. {@link OreVeinUtil#getVeinCenter} returns
     * empty for every other chunk without consuming any randomness, so looking those up only serves to fill the
     * caches with guaranteed-empty entries. At the default grid size of 3 this visits roughly a ninth of the
     * chunks the full square scan used to.
     *
     * <p>
     * The order is row-major (ascending z outer, ascending x inner) and must stay that way: the veins this
     * resolves are placed against a single shared {@code RandomSource} in {@link OrePlacer#placeVein}, so
     * reordering them would change generated output.
     */
    private Stream<ChunkPos> getSurroundingChunks(ChunkPos center, int searchDistance) {
        final int gridSize = OreVeinUtil.getVeinGridSize();

        final int maxX = center.x + searchDistance;
        final int maxZ = center.z + searchDistance;

        final int firstX = firstGridCoordAtOrAfter(center.x - searchDistance, gridSize);
        final int firstZ = firstGridCoordAtOrAfter(center.z - searchDistance, gridSize);

        return IntStream.iterate(firstZ, z -> z <= maxZ, z -> z + gridSize)
                .boxed()
                .flatMap(z -> IntStream.iterate(firstX, x -> x <= maxX, x -> x + gridSize)
                        .mapToObj(x -> new ChunkPos(x, z)));
    }

    /**
     * @return The smallest multiple of {@code gridSize} greater than or equal to {@code min}.
     */
    private static int firstGridCoordAtOrAfter(int min, int gridSize) {
        return Math.floorDiv(min + gridSize - 1, gridSize) * gridSize;
    }
}
