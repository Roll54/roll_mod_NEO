package com.roll_54.roll_mod.gtmogs.api.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import com.google.common.collect.HashBiMap;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.indicators.IndicatorGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins.NoopVeinGenerator;
import com.roll_54.roll_mod.gtmogs.data.tag.CustomTags;
import com.roll_54.roll_mod.gtmogs.utils.WeightedEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WorldGeneratorUtils {

    public static final RuleTest END_ORE_REPLACEABLES = new TagMatchTest(CustomTags.ENDSTONE_ORE_REPLACEABLES);
    public static final RuleTest MOON_ORE_REPLACEABLES = new TagMatchTest(CustomTags.MOONSTONE_ORE_REPLACEABLES);
    public static final RuleTest MARS_ORE_REPLACEABLES = new TagMatchTest(CustomTags.MARSSTONE_ORE_REPLACEABLES);
    public static final RuleTest MERCURY_ORE_REPLACEABLES = new TagMatchTest(CustomTags.MERCURYSTONE_ORE_REPLACEABLES);
    public static final RuleTest VENUS_ORE_REPLACEABLES = new TagMatchTest(CustomTags.VENUSSTONE_ORE_REPLACEABLES);
    public static final RuleTest GLACIO_ORE_REPLACEABLES = new TagMatchTest(CustomTags.GLACIOSTONE_ORE_REPLACEABLES);

    /**
     * Weak keys because nothing clears this on level unload -- {@code LevelEvent.Unload} only invalidates the
     * server vein cache -- so entries have to become collectable on their own. Wrapped in a synchronized view
     * rather than swapped for a {@link ConcurrentHashMap} so those weak-key semantics survive: this is written
     * from the worldgen worker threads, where an unguarded map resize can leave a thread spinning indefinitely.
     */
    private static final Map<ServerLevel, WorldOreVeinCache> oreVeinCache = Collections
            .synchronizedMap(new WeakHashMap<>());

    public static final SortedMap<String, IWorldGenLayer> WORLD_GEN_LAYERS = new Object2ObjectLinkedOpenHashMap<>();

    public static final HashBiMap<ResourceLocation, MapCodec<? extends VeinGenerator>> VEIN_GENERATORS = HashBiMap
            .create();
    public static final HashBiMap<ResourceLocation, Supplier<? extends VeinGenerator>> VEIN_GENERATOR_FUNCTIONS = HashBiMap
            .create();

    public static final HashBiMap<ResourceLocation, MapCodec<? extends IndicatorGenerator>> INDICATOR_GENERATORS = HashBiMap
            .create();
    public static final HashBiMap<ResourceLocation, Supplier<? extends IndicatorGenerator>> INDICATOR_GENERATOR_FUNCTIONS = HashBiMap
            .create();

    public record WeightedVein(Holder<OreVeinDefinition> vein, int weight) implements WeightedEntry {}

    private static class WorldOreVeinCache {

        private final List<Holder<OreVeinDefinition>> worldVeins;
        private final ResourceKey<Level> dimension;

        /**
         * Veins applicable to a biome, already split by world gen layer.
         *
         * <p>
         * Bucketing once per biome replaces re-filtering and re-allocating the whole biome list for each layer on
         * every vein center -- which matters more now that the layer count has grown past a handful.
         *
         * <p>
         * Concurrent because the worldgen worker threads share one instance of this per level.
         */
        private final Map<Holder<Biome>, Map<IWorldGenLayer, List<WeightedVein>>> biomeVeinsByLayer = new ConcurrentHashMap<>();

        public WorldOreVeinCache(ServerLevel level) {
            this.dimension = level.dimension();
            this.worldVeins = level.registryAccess().registryOrThrow(GTRegistries.ORE_VEIN_REGISTRY).holders()
                    .filter(vein -> !(vein.value().veinGenerator() instanceof NoopVeinGenerator))
                    .filter(entry -> entry.value().dimensionFilter().stream()
                            .anyMatch(dim -> WorldGeneratorUtils.isSameDimension(dim, level.dimension())))
                    .collect(Collectors.toList());
        }

        private List<WeightedVein> getEntry(Holder<Biome> biome, IWorldGenLayer layer) {
            return biomeVeinsByLayer
                    .computeIfAbsent(biome, this::bucketByLayer)
                    .getOrDefault(layer, List.of());
        }

        private Map<IWorldGenLayer, List<WeightedVein>> bucketByLayer(Holder<Biome> biome) {
            Map<IWorldGenLayer, List<WeightedVein>> byLayer = new Object2ObjectOpenHashMap<>();

            // Iterate in worldVeins order so each bucket keeps the same relative order the previous per-layer
            // filter produced: GTUtil.getRandomItem walks the list accumulating weights, so a different order
            // resolves a different vein for the same random value.
            for (Holder<OreVeinDefinition> vein : worldVeins) {
                if (!vein.value().isForBiome(dimension, biome)) continue;

                int weight = vein.value().weightForBiome(biome);
                if (weight <= 0) continue;

                byLayer.computeIfAbsent(vein.value().layer(dimension), l -> new ArrayList<>())
                        .add(new WeightedVein(vein, weight));
            }

            return byLayer;
        }
    }

    /**
     * @return The veins that can generate in the supplied biome on the supplied layer, in weight-stable order.
     *         Empty if none apply.
     */
    public static List<WeightedVein> getCachedBiomeVeins(ServerLevel level, Holder<Biome> biome,
                                                         IWorldGenLayer layer) {
        return oreVeinCache.computeIfAbsent(level, WorldOreVeinCache::new).getEntry(biome, layer);
    }

    public static Optional<String> getWorldGenLayerKey(IWorldGenLayer layer) {
        return WORLD_GEN_LAYERS.entrySet().stream()
                .filter(entry -> entry.getValue().equals(layer))
                .map(Entry::getKey)
                .findFirst();
    }

    public static boolean isSameDimension(ResourceKey<Level> first, ResourceKey<Level> second) {
        return first == second;
    }

    public static <T> Map<ChunkPos, Map<BlockPos, T>> groupByChunks(Map<BlockPos, T> input) {
        return input.entrySet().stream().collect(Collectors.groupingBy(
                entry -> new ChunkPos(entry.getKey()),
                Object2ObjectOpenHashMap::new,
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, Object2ObjectOpenHashMap::new)));
    }

    public static <T> Map<ChunkPos, List<BlockPos>> groupByChunks(Collection<BlockPos> positions) {
        return positions.stream().collect(Collectors.groupingBy(ChunkPos::new));
    }

    public static Collection<ChunkPos> getChunks(Collection<BlockPos> positions) {
        return positions.stream()
                .collect(Collectors.groupingBy(ChunkPos::new))
                .keySet();
    }

    public static void generateChunks(WorldGenLevel level, ChunkStatus requiredStatus, Collection<ChunkPos> chunks) {
        List<ChunkPos> previouslyUnloadedChunks = new ObjectArrayList<>();
        var chunkSource = level.getChunkSource();

        for (ChunkPos chunkPos : chunks) {
            var chunk = chunkSource.getChunk(chunkPos.x, chunkPos.z, false);

            if (chunk == null) {
                previouslyUnloadedChunks.add(chunkPos);
            }

            chunkSource.getChunk(chunkPos.x, chunkPos.z, requiredStatus, true);
        }

        if (level instanceof ServerLevel serverLevel) {
            previouslyUnloadedChunks.forEach(chunk -> serverLevel.unload(serverLevel.getChunk(chunk.x, chunk.z)));
        }
    }

    public static Optional<BlockPos> findBlockPos(BlockPos initialPos, Predicate<BlockPos> predicate,
                                                  Consumer<BlockPos.MutableBlockPos> step, int maxSteps) {
        var currentPos = initialPos.mutable();

        while (maxSteps-- >= 0) {
            step.accept(currentPos);

            if (predicate.test(currentPos))
                return Optional.of(currentPos.immutable());
        }

        return Optional.empty();
    }

    public static void invalidateOreVeinCache() {
        oreVeinCache.clear();
    }
}
