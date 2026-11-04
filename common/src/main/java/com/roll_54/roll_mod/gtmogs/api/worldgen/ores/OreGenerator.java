package com.roll_54.roll_mod.gtmogs.api.worldgen.ores;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.worldgen.IWorldGenLayer;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.WorldGeneratorUtils;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.indicators.IndicatorGenerator;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.integration.map.cache.server.ServerCache;
import com.roll_54.roll_mod.gtmogs.utils.GTUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Responsible for (pre)generating ore veins.<br>
 * This does not actually place any of the ore's blocks, and delegates to the applicable vein generator.
 */
public class OreGenerator {

    public record VeinConfiguration(GeneratedVeinMetadata data, RandomSource random) {

        public RandomSource newRandom() {
            return new XoroshiroRandomSource(random.nextLong());
        }
    }

    public List<GeneratedVeinMetadata> generateMetadata(final WorldGenLevel level, ChunkGenerator chunkGenerator,
                                                        ChunkPos chunkPos) {
        long started = OreGenDiagnostics.start();
        try {
            return generateMetadata0(level, chunkGenerator, chunkPos);
        } finally {
            OreGenDiagnostics.record(OreGenDiagnostics.Phase.VEIN_METADATA, started);
        }
    }

    private List<GeneratedVeinMetadata> generateMetadata0(final WorldGenLevel level, ChunkGenerator chunkGenerator,
                                                          ChunkPos chunkPos) {
        return createConfigs(level, chunkGenerator, chunkPos).stream()
                .map(OreGenerator::logVeinGeneration)
                .map(entry -> entry.data)
                .peek(data -> {
                    int gridX = Math.floorDiv(chunkPos.x, ConfigHolder.INSTANCE.worldgen.oreVeins.oreVeinGridSize);
                    int gridZ = Math.floorDiv(chunkPos.z, ConfigHolder.INSTANCE.worldgen.oreVeins.oreVeinGridSize);
                    ServerCache.instance.addVein(level.getLevel().dimension(), gridX, gridZ, data);
                })
                .toList();
    }

    /**
     * Generates the vein for the specified chunk metadata.<br>
     * If the chunk is not located on one of the ore vein grid's intersections, no vein will be generated.
     *
     * <p>
     * Note that depending on the configured random offset, the actual center of the generated vein may be located
     * outside the specified origin chunk.
     *
     * @return The generated vein for the specified chunk metadata.<br>
     *         {@code Optional.empty()} if no vein exists at this chunk.
     */
    public List<GeneratedVein> generateOres(WorldGenLevel level, List<GeneratedVeinMetadata> metadata,
                                            ChunkPos chunkPos) {
        return metadata.stream()
                .map(data -> new VeinConfiguration(
                        data,
                        new XoroshiroRandomSource(level.getSeed() ^ chunkPos.toLong())))
                .flatMap(config -> generateOres(config, level, chunkPos).stream())
                .toList();
    }

    /**
     * Salt mixed into the indicator RNG so that indicator positions are decorrelated from ore positions
     * (which use the same {@code level seed ^ chunkPos} base).
     */
    private static final long INDICATOR_SALT = 0x9E3779B97F4A7C15L;

    /**
     * Generates the surface indicators for the supplied chunk metadata, reusing the veins' centers.
     *
     * @return One {@link GeneratedVein} per vein that produced any indicators.
     */
    public List<GeneratedVein> generateIndicators(WorldGenLevel level, List<GeneratedVeinMetadata> metadata,
                                                  ChunkPos chunkPos) {
        return metadata.stream()
                .filter(data -> !data.definition().value().indicatorGenerators().isEmpty())
                .flatMap(data -> {
                    long started = OreGenDiagnostics.start();
                    OreVeinDefinition definition = data.definition().value();
                    var random = new XoroshiroRandomSource(level.getSeed() ^ chunkPos.toLong() ^ INDICATOR_SALT);

                    Map<BlockPos, OreBlockPlacer> generated = new Object2ObjectOpenHashMap<>();
                    for (IndicatorGenerator indicator : definition.indicatorGenerators()) {
                        generated.putAll(indicator.generate(level, new XoroshiroRandomSource(random.nextLong()),
                                definition, data.center()));
                    }

                    OreGenDiagnostics.record(OreGenDiagnostics.Phase.INDICATORS, started);

                    return generated.isEmpty() ? Stream.<GeneratedVein>empty() :
                            Stream.of(new GeneratedVein(chunkPos, definition.layer(level.getLevel().dimension()),
                                    generated));
                })
                .toList();
    }

    public Optional<GeneratedVein> generateOres(VeinConfiguration config, WorldGenLevel level, ChunkPos chunkPos) {
        OreVeinDefinition definition = config.data.definition().value();

        long started = OreGenDiagnostics.start();
        Map<BlockPos, OreBlockPlacer> generatedVeins = definition.veinGenerator()
                .generate(level, config.newRandom(), definition, config.data.center());
        OreGenDiagnostics.record(OreGenDiagnostics.Phase.VEIN_SHAPE, started);

        if (generatedVeins.isEmpty()) {
            logEmptyVein(config);
            return Optional.empty();
        }

        return Optional.of(new GeneratedVein(chunkPos, definition.layer(level.getLevel().dimension()),
                generatedVeins));
    }

    private List<VeinConfiguration> createConfigs(WorldGenLevel level, ChunkGenerator generator, ChunkPos chunkPos) {
        var random = new XoroshiroRandomSource(level.getSeed() ^ chunkPos.toLong());

        return OreVeinUtil.getVeinCenter(chunkPos, random).stream()
                .flatMap(veinCenter -> getEntries(level, veinCenter, random).map(entry -> {
                    if (entry == null) return null;

                    BlockPos origin = computeVeinOrigin(level, generator, chunkPos, random, veinCenter, entry.value())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Cannot determine y coordinate for the vein at " + veinCenter));

                    return new VeinConfiguration(new GeneratedVeinMetadata(chunkPos, origin, entry), random);
                })).toList();
    }

    private Stream<Holder<OreVeinDefinition>> getEntries(WorldGenLevel level, BlockPos veinCenter,
                                                         XoroshiroRandomSource random) {
        // Sampled once instead of once per layer: the coordinates never varied between iterations, so every
        // applicable layer was re-resolving the exact same biome.
        int quartX = QuartPos.fromBlock(veinCenter.getX());
        int quartY = QuartPos.fromBlock(veinCenter.getY());
        int quartZ = QuartPos.fromBlock(veinCenter.getZ());
        Holder<Biome> biome = level.getUncachedNoiseBiome(quartX, quartY, quartZ);

        return WorldGeneratorUtils.WORLD_GEN_LAYERS.values().stream()
                .filter(layer -> layer.isApplicableForLevel(level.getLevel().dimension()))
                .map(layer -> getEntry(level, biome, random, layer))
                .filter(Objects::nonNull);
    }

    @Nullable
    private Holder<OreVeinDefinition> getEntry(WorldGenLevel level, Holder<Biome> biome, RandomSource random,
                                               IWorldGenLayer layer) {
        var veins = WorldGeneratorUtils.getCachedBiomeVeins(level.getLevel(), biome, layer);
        var randomVein = GTUtil.getRandomItem(random, veins);
        return randomVein == null ? null : randomVein.vein();
    }

    @NotNull
    private static Optional<BlockPos> computeVeinOrigin(WorldGenLevel level, ChunkGenerator generator, ChunkPos pos,
                                                        RandomSource random, BlockPos veinCenter,
                                                        OreVeinDefinition entry) {
        ResourceKey<Level> dimension = level.getLevel().dimension();
        int layerSeed = WorldGeneratorUtils.getWorldGenLayerKey(entry.layer(dimension))
                .map(String::hashCode)
                .orElse(0);
        var layeredRandom = new XoroshiroRandomSource(random.nextLong() ^ ((long) layerSeed));

        veinCenter = OreVeinUtil.getVeinCenter(pos, layeredRandom).orElse(veinCenter);
        return entry.heightRange(dimension).getPositions(
                new PlacementContext(level, generator, Optional.empty()),
                layeredRandom, veinCenter).findFirst();
    }

    /////////////////////////////////////
    // ********* LOGGING *********//
    /////////////////////////////////////

    private static VeinConfiguration logVeinGeneration(VeinConfiguration config) {
        if (ConfigHolder.INSTANCE.dev.debugWorldgen) {
            GTMOGS.LOGGER.debug("Generating vein {} at {}",
                    config.data.definition().getKey().location(), config.data.center());
        }

        return config;
    }

    private static void logEmptyVein(VeinConfiguration config) {
        if (ConfigHolder.INSTANCE.dev.debugWorldgen) {
            GTMOGS.LOGGER.debug("No blocks generated for vein {} at {}",
                    config.data.definition().getKey().location(), config.data.center());
        }
    }
}
