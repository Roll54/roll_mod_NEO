package com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins;

import com.roll_54.roll_mod.compat.gtmogs.OreStoneVariants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreBlockPlacer;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreVeinUtil;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTDensityFunctions;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;
import com.roll_54.roll_mod.gtmogs.utils.GTUtil;
import com.roll_54.roll_mod.gtmogs.utils.WeightedEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true, chain = true)
public class VeinedVeinGenerator extends VeinGenerator {

    public static final Codec<List<TargetBlockState>> BLOCK_ENTRY_CODEC = TargetBlockState.CODEC.listOf();
    // spotless:off
    public static final MapCodec<VeinedVeinGenerator> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            VeinBlockDefinition.CODEC.listOf().fieldOf("ore_blocks").forGetter(it -> it.oreBlocks),
            VeinBlockDefinition.CODEC.listOf().fieldOf("rare_blocks").forGetter(it -> it.rareBlocks),
            BlockState.CODEC.fieldOf("filler_block").orElse(Blocks.AIR.defaultBlockState()).forGetter(it -> it.fillerBlock),
            Codec.INT.fieldOf("min_y").forGetter(it -> it.minYLevel),
            Codec.INT.fieldOf("max_y").forGetter(it -> it.maxYLevel),
            Codec.FLOAT.fieldOf("veininess_threshold").orElse(0.4f).forGetter(it -> it.veininessThreshold),
            Codec.INT.fieldOf("edge_roundoff_begin").orElse(20).forGetter(it -> it.edgeRoundoffBegin),
            Codec.DOUBLE.fieldOf("max_edge_roundoff").orElse(0.2).forGetter(it -> it.maxEdgeRoundoff),
            Codec.FLOAT.fieldOf("min_richness").orElse(0.1f).forGetter(it -> it.minRichness),
            Codec.FLOAT.fieldOf("max_richness").orElse(0.3f).forGetter(it -> it.maxRichness),
            Codec.FLOAT.fieldOf("max_richness_threshold").orElse(0.6f).forGetter(it -> it.maxRichnessThreshold),
            Codec.FLOAT.fieldOf("rare_block_chance").orElse(0.02f).forGetter(it -> it.rareBlockChance)
    ).apply(instance, VeinedVeinGenerator::new));
    // spotless:on
    public List<VeinBlockDefinition> oreBlocks = new ArrayList<>();
    public List<VeinBlockDefinition> rareBlocks = new ArrayList<>();
    @Setter
    public BlockState fillerBlock = Blocks.AIR.defaultBlockState();
    @Setter
    public int minYLevel;
    @Setter
    public int maxYLevel;
    @Setter
    public float veininessThreshold = 0.4f;
    @Setter
    public int edgeRoundoffBegin = 20;
    @Setter
    public double maxEdgeRoundoff = 0.2;
    @Setter
    public float minRichness = 0.1f;
    @Setter
    public float maxRichness = 0.3f;
    @Setter
    public float maxRichnessThreshold = 0.6f;
    @Setter
    public float rareBlockChance = 0.02f;

    @Override
    public List<VeinEntry> getAllEntries() {
        List<VeinEntry> entries = new ArrayList<>(oreBlocks.size() + rareBlocks.size());
        for (var def : oreBlocks) {
            VeinGenerator.mapTarget(def.block, def.weight).forEach(entries::add);
        }
        for (var def : rareBlocks) {
            VeinGenerator.mapTarget(def.block, def.weight).forEach(entries::add);
        }
        return entries;
    }

    @Override
    public Map<BlockPos, OreBlockPlacer> generate(WorldGenLevel level, RandomSource random, OreVeinDefinition entry,
                                                  BlockPos origin) {
        Map<BlockPos, OreBlockPlacer> generatedBlocks = new Object2ObjectOpenHashMap<>();

        Registry<DensityFunction> densityFunctions = level.registryAccess()
                .registryOrThrow(Registries.DENSITY_FUNCTION);

        RandomState randomState = level.getLevel().getChunkSource().randomState();
        Blender blender;
        if (level instanceof WorldGenRegion region) {
            blender = Blender.of(region);
        } else {
            blender = Blender.empty();
        }

        DensityFunction veinToggle = mapToNoise(
                resolveVeinFunction(level, densityFunctions, GTDensityFunctions.NEW_ORE_VEIN_TOGGLE,
                        GTDensityFunctions::veinToggle),
                randomState);
        DensityFunction veinRidged = mapToNoise(
                resolveVeinFunction(level, densityFunctions, GTDensityFunctions.NEW_ORE_VEIN_RIDGED,
                        GTDensityFunctions::veinRidged),
                randomState);

        int size = entry.clusterSize().sample(random);

        int radius = Mth.ceil(size / 2f);

        int randOffsetX = random.nextInt(16);
        int randOffsetY = random.nextInt(16);
        int randOffsetZ = random.nextInt(16);

        // The vertical guard and the edge roundoff derive only from the vein's origin, which is fixed for the
        // entire sweep, so they are hoisted out of the loop. A vein whose origin sits outside the layer's height
        // range can now bail out immediately instead of running a full (size+1)^3 noise sweep and discarding
        // every position -- for a size-64 vein that was ~275k wasted multi-octave noise evaluations producing no
        // blocks at all. Placed after the offset draws above so the random stream is unchanged either way.
        int blockY = origin.getY();
        int minY = blockY - this.minYLevel;
        int maxY = this.maxYLevel - blockY;
        if (minY < 0 || maxY < 0) {
            return generatedBlocks;
        }

        int lowY = Math.min(maxY, minY);
        double edgeRoundoff = Mth.clampedMap(lowY, 0.0, edgeRoundoffBegin, -maxEdgeRoundoff, 0.0);

        var posMin = origin.offset(-radius, -radius, -radius);
        var posMax = origin.offset(+radius, +radius, +radius);

        // One reusable context instead of an allocation per visited position; DensityFunction.compute does not
        // retain it beyond the call.
        MutableFunctionContext functionContext = new MutableFunctionContext(blender);

        for (BlockPos chunkedPos : BlockPos.betweenClosed(posMin, posMax)) {
            final int x = chunkedPos.getX();
            final int y = chunkedPos.getY();
            final int z = chunkedPos.getZ();

            functionContext.x = x + randOffsetX;
            functionContext.y = y + randOffsetY;
            functionContext.z = z + randOffsetZ;

            double absToggleNoise = Math.abs(veinToggle.compute(functionContext));
            if (absToggleNoise + edgeRoundoff < veininessThreshold) {
                continue;
            }
            if (random.nextFloat() > entry.density()) {
                continue;
            }
            if (veinRidged.compute(functionContext) >= 0.0) {
                continue;
            }
            double chance = Mth.clampedMap(absToggleNoise, veininessThreshold, maxRichnessThreshold, minRichness,
                    maxRichness);

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);

            final var randomSeed = random.nextLong(); // Fully deterministic regardless of chunk order
            generatedBlocks.put(pos, (access, section) -> placeBlock(access, section, randomSeed, entry, chance,
                    rareBlocks, pos, oreBlocks));
        }

        return generatedBlocks;
    }

    /**
     * Mutable {@link DensityFunction.FunctionContext} reused across a vein's sweep.
     *
     * <p>
     * The sweep visits {@code (size+1)^3} positions, so allocating a context per position was a meaningful source
     * of garbage on the worldgen worker threads.
     */
    private static final class MutableFunctionContext implements DensityFunction.FunctionContext {

        private final Blender blender;

        private int x;
        private int y;
        private int z;

        private MutableFunctionContext(Blender blender) {
            this.blender = blender;
        }

        @Override
        public int blockX() {
            return x;
        }

        @Override
        public int blockY() {
            return y;
        }

        @Override
        public int blockZ() {
            return z;
        }

        @Override
        public Blender getBlender() {
            return blender;
        }
    }

    private void placeBlock(BulkSectionAccess access, LevelChunkSection section, long randomSeed,
                            OreVeinDefinition entry,
                            double chance, List<VeinBlockDefinition> rareEntries,
                            BlockPos.MutableBlockPos pos,
                            List<VeinBlockDefinition> commonEntries) {
        RandomSource random = new XoroshiroRandomSource(randomSeed);
        int sectionX = SectionPos.sectionRelative(pos.getX());
        int sectionY = SectionPos.sectionRelative(pos.getY());
        int sectionZ = SectionPos.sectionRelative(pos.getZ());

        BlockState current = section.getBlockState(sectionX, sectionY, sectionZ);
        if (random.nextFloat() <= entry.density()) {
            if (random.nextFloat() < chance) {
                if (rareBlocks != null && !rareBlocks.isEmpty() && random.nextFloat() < rareBlockChance) {
                    var ore = GTUtil.getRandomItem(random, rareEntries);
                    if (ore == null) return;
                    placeOre(ore.block, current, access, section, random, pos, entry);
                } else {
                    var ore = GTUtil.getRandomItem(random, commonEntries);
                    if (ore == null) return;
                    placeOre(ore.block, current, access, section, random, pos, entry);
                }
            } else {
                if (fillerBlock == null || fillerBlock.isAir())
                    return;
                if (!OreVeinUtil.canPlaceOre(current, access::getBlockState, random, entry, pos))
                    return;
                section.setBlockState(sectionX, sectionY, sectionZ, fillerBlock, false);
            }
        }
    }

    protected static void placeOre(List<TargetBlockState> block, BlockState current,
                                   BulkSectionAccess level, LevelChunkSection section, RandomSource random,
                                   BlockPos.MutableBlockPos pos, OreVeinDefinition entry) {
        int x = SectionPos.sectionRelative(pos.getX());
        int y = SectionPos.sectionRelative(pos.getY());
        int z = SectionPos.sectionRelative(pos.getZ());

        for (TargetBlockState targetState : block) {
            if (!OreVeinUtil.canPlaceOre(current, level::getBlockState, random, entry, targetState, pos))
                continue;
            if (targetState.state.isAir())
                continue;
            // roll_mod: the same ore in the rock it lands in (granite hematite in granite), see OreStoneVariants.
            section.setBlockState(x, y, z,
                        OreStoneVariants.forRock(targetState.state, section.getBlockState(x, y, z)), false);
            break;
        }
    }

    @Override
    public VeinGenerator build() {
        return this;
    }

    @Override
    public VeinGenerator copy() {
        return new VeinedVeinGenerator(new ArrayList<>(this.oreBlocks), new ArrayList<>(this.rareBlocks),
                this.fillerBlock, this.minYLevel, this.maxYLevel, this.veininessThreshold, this.edgeRoundoffBegin,
                this.maxEdgeRoundoff, this.minRichness, this.maxRichness, this.maxRichnessThreshold,
                this.rareBlockChance);
    }

    @Override
    public MapCodec<? extends VeinGenerator> codec() {
        return CODEC;
    }

    public VeinedVeinGenerator oreBlock(BlockState blockState, int weight) {
        TargetBlockState target = OreConfiguration.target(AlwaysTrueTest.INSTANCE, blockState);
        return this.oreBlock(new VeinBlockDefinition(List.of(target), weight));
    }

    public VeinedVeinGenerator oreBlock(VeinBlockDefinition material) {
        for (Block b : material.block().stream().map(vein -> vein.state.getBlock()).toList()) {
            GTOreVeins.addVeinOre(b);
        }
        this.oreBlocks.add(material);
        return this;
    }

    public VeinedVeinGenerator rareBlock(BlockState blockState, int weight) {
        GTOreVeins.addVeinOre(blockState.getBlock());
        TargetBlockState target = OreConfiguration.target(AlwaysTrueTest.INSTANCE, blockState);
        return this.rareBlock(new VeinBlockDefinition(List.of(target), weight));
    }

    public VeinedVeinGenerator rareBlock(VeinBlockDefinition material) {
        for (Block b : material.block().stream().map(vein -> vein.state.getBlock()).toList()) {
            GTOreVeins.addVeinOre(b);
        }
        this.rareBlocks.add(material);
        return this;
    }

    public record VeinBlockDefinition(List<TargetBlockState> block, int weight)
            implements WeightedEntry {

        public static final Codec<VeinBlockDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BLOCK_ENTRY_CODEC.fieldOf("block").forGetter(x -> x.block),
                Codec.INT.fieldOf("weight").forGetter(x -> x.weight))
                .apply(instance, VeinBlockDefinition::new));
    }

    private static final Set<ResourceKey<DensityFunction>> LOGGED_FALLBACKS = ConcurrentHashMap.newKeySet();

    /**
     * Resolves one of the vein density functions.
     *
     * <p>
     * These are datagen'd into {@code data/gtmogs/worldgen/density_function/}, but a jar built without running
     * datagen — or a world whose datapacks don't carry them — leaves the registry entry absent. Rather than
     * silently generating nothing (a zero density function never clears {@code veininess_threshold}), rebuild the
     * definition from the vanilla noise parameters, which every dimension has.
     */
    @Nullable
    private static DensityFunction resolveVeinFunction(WorldGenLevel level, Registry<DensityFunction> densityFunctions,
                                                       ResourceKey<DensityFunction> key,
                                                       Function<GTDensityFunctions.NoiseLookup, DensityFunction> fallback) {
        DensityFunction registered = densityFunctions.get(key);
        if (registered != null)
            return registered;

        var noises = level.registryAccess().registry(Registries.NOISE).orElse(null);
        if (noises == null)
            return null;

        if (LOGGED_FALLBACKS.add(key)) {
            GTMOGS.LOGGER.warn("Density function {} is not registered; falling back to the built-in definition. " +
                    "Run datagen when building GTMOGS to ship it properly.", key.location());
        }

        return fallback.apply(noises::getHolderOrThrow);
    }

    private static DensityFunction mapToNoise(@Nullable DensityFunction function, RandomState randomState) {
        if (function == null) {
            // if the function is somehow missing, default to 0 and print an error.
            GTMOGS.LOGGER.error("Failed to generate veined vein, missing required density (3D noise) functions!");
            return DensityFunctions.zero();
        }

        return function.mapAll(new DensityFunction.Visitor() {

            @Override
            public @NotNull DensityFunction apply(@NotNull DensityFunction densityFunction) {
                return densityFunction;
            }

            @Override
            public DensityFunction.@NotNull NoiseHolder visitNoise(DensityFunction.@NotNull NoiseHolder noiseHolder) {
                var holder = noiseHolder.noiseData();
                var noise = randomState.getOrCreateNoise(holder.unwrapKey().orElseThrow());
                return new DensityFunction.NoiseHolder(holder, noise);
            }
        });
    }
}
