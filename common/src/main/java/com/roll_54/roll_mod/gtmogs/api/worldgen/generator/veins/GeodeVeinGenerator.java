package com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.feature.GeodeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreBlockPlacer;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;
import com.roll_54.roll_mod.gtmogs.utils.GTUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Accessors(chain = true, fluent = true)
@AllArgsConstructor
@NoArgsConstructor
public class GeodeVeinGenerator extends VeinGenerator {

    public static final Codec<Double> CHANCE_RANGE = Codec.doubleRange(0.0, 1.0);
    public static final Codec<IntProvider> SIZE_PROVIDER = IntProvider.codec(1, 20);
    // spotless:off
    public static final MapCodec<GeodeVeinGenerator> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
                    GeodeBlockSettings.CODEC.fieldOf("blocks").forGetter((config) -> config.geodeBlockSettings),
                    GeodeLayerSettings.CODEC.fieldOf("layers").forGetter((config) -> config.geodeLayerSettings),
                    GeodeCrackSettings.CODEC.fieldOf("crack").forGetter((config) -> config.geodeCrackSettings),
                    CHANCE_RANGE.fieldOf("use_potential_placements_chance").orElse(0.35).forGetter((config) -> config.usePotentialPlacementsChance),
                    CHANCE_RANGE.fieldOf("use_alternate_layer0_chance").orElse(0.0).forGetter((config) -> config.useAlternateLayer0Chance),
                    Codec.BOOL.fieldOf("placements_require_layer0_alternate").orElse(true).forGetter((config) -> config.placementsRequireLayer0Alternate),
                    SIZE_PROVIDER.fieldOf("outer_wall_distance").orElse(UniformInt.of(4, 5)).forGetter((config) -> config.outerWallDistance),
                    SIZE_PROVIDER.fieldOf("distribution_points").orElse(UniformInt.of(3, 4)).forGetter((config) -> config.distributionPoints),
                    SIZE_PROVIDER.fieldOf("point_offset").orElse(UniformInt.of(1, 2)).forGetter((config) -> config.pointOffset),
                    Codec.INT.fieldOf("min_gen_offset").orElse(-16).forGetter((config) -> config.minGenOffset),
                    Codec.INT.fieldOf("max_gen_offset").orElse(16).forGetter((config) -> config.maxGenOffset),
                    CHANCE_RANGE.fieldOf("noise_multiplier").orElse(0.05).forGetter((config) -> config.noiseMultiplier),
                    Codec.INT.fieldOf("invalid_blocks_threshold").forGetter((config) -> config.invalidBlocksThreshold)
    ).apply(instance, GeodeVeinGenerator::new));
    // spotless:on
    @Setter
    public GeodeBlockSettings geodeBlockSettings;
    @Setter
    public GeodeLayerSettings geodeLayerSettings;
    @Setter
    public GeodeCrackSettings geodeCrackSettings;
    @Setter
    public double usePotentialPlacementsChance = 0.5;
    @Setter
    public double useAlternateLayer0Chance = 0.0;
    @Setter
    public boolean placementsRequireLayer0Alternate = false;
    @Setter
    public IntProvider outerWallDistance = ConstantInt.of(0);
    @Setter
    public IntProvider distributionPoints = ConstantInt.of(0);
    @Setter
    public IntProvider pointOffset = ConstantInt.of(0);
    @Setter
    public int minGenOffset = 0;
    @Setter
    public int maxGenOffset = 0;
    @Setter
    public double noiseMultiplier = 1.0;
    @Setter
    public int invalidBlocksThreshold = 0;

    @Override
    public List<VeinEntry> getAllEntries() {
        RandomSource source = new LegacyRandomSource(0);
        return List.of(
                new VeinEntry(geodeBlockSettings.fillingProvider.getState(source, BlockPos.ZERO), 1),
                new VeinEntry(geodeBlockSettings.innerLayerProvider.getState(source, BlockPos.ZERO), 1),
                new VeinEntry(geodeBlockSettings.alternateInnerLayerProvider.getState(source, BlockPos.ZERO), 1),
                new VeinEntry(geodeBlockSettings.middleLayerProvider.getState(source, BlockPos.ZERO), 1),
                new VeinEntry(geodeBlockSettings.outerLayerProvider.getState(source, BlockPos.ZERO), 1));
    }

    @Override
    public Map<BlockPos, OreBlockPlacer> generate(WorldGenLevel level, RandomSource random, OreVeinDefinition entry,
                                                  BlockPos origin) {
        // The BulkSectionAccess is only read from here -- the geode's shape depends on what is already in the
        // world -- and the resulting blocks are returned as placers for OrePlacer to write later, once the chunk
        // that owns each position is the one being generated.
        //
        // Writing during generation is what this used to do, and it cannot work: a vein is generated while some
        // chunk within the vein search radius is being decorated, but the FEATURES step only permits block writes
        // within one chunk of the chunk being decorated (ChunkPyramid: blockStateWriteRadius(1)) while permitting
        // reads out to eight. Everything beyond that was rejected by WorldGenRegion#ensureCanWrite, which logged
        // "Detected setBlock in a far chunk" per block and silently truncated the geode.
        //
        // The access MUST be closed on every path. It acquires each LevelChunkSection it touches, and an
        // unreleased section leaves its PalettedContainer semaphore permanently held -- any later acquirer parks
        // forever, including vanilla chunk saving (PalettedContainer.pack) and terrain generation
        // (NoiseBasedChunkGenerator.fillFromNoise). That presents as a server that stops ticking and never
        // recovers, with no deadlock reported, because a leaked permit is not a lock cycle.
        try (BulkSectionAccess access = new BulkSectionAccess(level)) {
            return buildGeode(level, random, origin, access);
        }
    }

    private Map<BlockPos, OreBlockPlacer> buildGeode(WorldGenLevel level, RandomSource random, BlockPos origin,
                                                     BulkSectionAccess access) {
        BlockState blockState;
        int offset;
        int offset2;
        int minOffset = this.minGenOffset;
        int maxOffset = this.maxGenOffset;
        int distributionSample = this.distributionPoints.sample(random);
        List<ObjectIntPair<BlockPos>> points = new ArrayList<>(distributionSample);
        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(level.getSeed()));
        NormalNoise normalNoise = NormalNoise.create(worldgenRandom, -4, 1.0);
        List<BlockPos> list2 = new ArrayList<>(3);
        double wallDistance = (double) distributionSample / (double) this.outerWallDistance.getMaxValue();
        double fillingSize = 1.0 / Math.sqrt(geodeLayerSettings.filling);
        double innerSize = 1.0 / Math.sqrt(geodeLayerSettings.innerLayer + wallDistance);
        double middleSize = 1.0 / Math.sqrt(geodeLayerSettings.middleLayer + wallDistance);
        double outerSize = 1.0 / Math.sqrt(geodeLayerSettings.outerLayer + wallDistance);
        double crackSize = 1.0 / Math.sqrt(geodeCrackSettings.baseCrackSize + random.nextDouble() / 2.0 +
                (distributionSample > 3 ? wallDistance : 0.0));
        boolean doCrack = (double) random.nextFloat() < geodeCrackSettings.generateCrackChance;
        int invalidBlocksCount = 0;
        for (offset2 = 0; offset2 < distributionSample; ++offset2) {
            offset = this.outerWallDistance.sample(random);
            BlockPos origin2 = origin.offset(offset, this.outerWallDistance.sample(random),
                    this.outerWallDistance.sample(random));
            blockState = access.getBlockState(origin2);
            if ((blockState.isAir() || blockState.is(BlockTags.GEODE_INVALID_BLOCKS)) &&
                    ++invalidBlocksCount > this.invalidBlocksThreshold) {
                return Map.of();
            }
            points.add(ObjectIntPair.of(origin2, this.pointOffset.sample(random)));
        }
        if (doCrack) {
            offset2 = random.nextInt(4);
            offset = distributionSample * 2 + 1;
            if (offset2 == 0) {
                list2.add(origin.offset(offset, 7, 0));
                list2.add(origin.offset(offset, 5, 0));
                list2.add(origin.offset(offset, 1, 0));
            } else if (offset2 == 1) {
                list2.add(origin.offset(0, 7, offset));
                list2.add(origin.offset(0, 5, offset));
                list2.add(origin.offset(0, 1, offset));
            } else if (offset2 == 2) {
                list2.add(origin.offset(offset, 7, offset));
                list2.add(origin.offset(offset, 5, offset));
                list2.add(origin.offset(offset, 1, offset));
            } else {
                list2.add(origin.offset(0, 7, 0));
                list2.add(origin.offset(0, 5, 0));
                list2.add(origin.offset(0, 1, 0));
            }
        }
        List<BlockPos> positions = new ArrayList<>();
        // Resolved shape, position -> state. Kept as states rather than placers so the second pass below can see
        // what the first one decided, the way it used to see the first pass's writes in the world.
        Map<BlockPos, BlockState> pending = new Object2ObjectOpenHashMap<>();
        Predicate<BlockState> placementPredicate = GeodeFeature.isReplaceable(this.geodeBlockSettings.cannotReplace);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(minOffset, minOffset, minOffset),
                origin.offset(maxOffset, maxOffset, maxOffset))) {
            double noiseValue = normalNoise.getValue(pos.getX(), pos.getY(), pos.getZ()) * this.noiseMultiplier;
            double s = 0.0;
            double t = 0.0;
            for (var pair : points) {
                s += Mth.invSqrt(pos.distSqr(pair.first()) + (double) pair.secondInt()) + noiseValue;
            }
            for (BlockPos origin4 : list2) {
                t += Mth.invSqrt(pos.distSqr(origin4) + (double) geodeCrackSettings.crackPointOffset) + noiseValue;
            }
            if (s < outerSize) continue;
            if (doCrack && t >= crackSize && s < fillingSize) {
                // The fluid ticks vanilla's GeodeFeature schedules around a crack are not scheduled here: the
                // level that generated the geode is a region centred on a different chunk than the one that will
                // place these blocks, so it is the wrong handle to schedule against. A fluid neighbouring a crack
                // therefore only starts flowing on its first block update rather than immediately.
                pending.put(pos.immutable(), Blocks.AIR.defaultBlockState());
                continue;
            }
            if (s >= fillingSize) {
                pending.put(pos.immutable(), geodeBlockSettings.fillingProvider.getState(random, pos));
                continue;
            }
            if (s >= innerSize) {
                boolean useAltLayer = (double) random.nextFloat() < this.useAlternateLayer0Chance;
                pending.put(pos.immutable(), useAltLayer ?
                        geodeBlockSettings.alternateInnerLayerProvider.getState(random, pos) :
                        geodeBlockSettings.innerLayerProvider.getState(random, pos));
                if (this.placementsRequireLayer0Alternate && !useAltLayer ||
                        !((double) random.nextFloat() < this.usePotentialPlacementsChance))
                    continue;
                positions.add(pos.immutable());
                continue;
            }
            if (s >= middleSize) {
                pending.put(pos.immutable(), geodeBlockSettings.middleLayerProvider.getState(random, pos));
                continue;
            }
            if (!(s >= outerSize)) continue;
            pending.put(pos.immutable(), geodeBlockSettings.outerLayerProvider.getState(random, pos));
        }
        List<BlockState> innerPlacements = geodeBlockSettings.innerPlacements;
        block5:
        for (BlockPos origin2 : positions) {
            blockState = Util.getRandom(innerPlacements, random);
            for (Direction direction2 : GTUtil.DIRECTIONS) {
                if (blockState.hasProperty(BlockStateProperties.FACING)) {
                    blockState = blockState.setValue(BlockStateProperties.FACING, direction2);
                }
                BlockPos origin6 = origin2.relative(direction2).immutable();
                // Prefer what the first pass decided for this position; it has not been written to the world yet.
                BlockState blockState2 = pending.getOrDefault(origin6, access.getBlockState(origin6));
                if (blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    blockState = blockState.setValue(BlockStateProperties.WATERLOGGED,
                            blockState2.getFluidState().isSource());
                }
                if (!BuddingAmethystBlock.canClusterGrowAtState(blockState2)) continue;
                pending.put(origin6, blockState);
                continue block5;
            }
        }

        Map<BlockPos, OreBlockPlacer> generated = new Object2ObjectOpenHashMap<>(pending.size());
        pending.forEach((pos, state) -> generated.put(pos, (access2, section) -> {
            // Same guard safeSetBlock used to apply, just deferred to the moment of the write.
            if (!placementPredicate.test(access2.getBlockState(pos)))
                return;
            section.setBlockState(
                    SectionPos.sectionRelative(pos.getX()),
                    SectionPos.sectionRelative(pos.getY()),
                    SectionPos.sectionRelative(pos.getZ()),
                    state, false);
        }));
        return generated;
    }

    @Override
    public VeinGenerator build() {
        return this;
    }

    @Override
    public VeinGenerator copy() {
        return new GeodeVeinGenerator(this.geodeBlockSettings, this.geodeLayerSettings, this.geodeCrackSettings,
                this.usePotentialPlacementsChance, this.useAlternateLayer0Chance, this.placementsRequireLayer0Alternate,
                this.outerWallDistance, this.distributionPoints, this.pointOffset, this.minGenOffset, this.maxGenOffset,
                this.noiseMultiplier, this.invalidBlocksThreshold);
    }

    @Override
    public MapCodec<? extends VeinGenerator> codec() {
        return CODEC;
    }

    public record GeodeBlockSettings(BlockStateProvider fillingProvider,
                                     BlockStateProvider innerLayerProvider,
                                     BlockStateProvider alternateInnerLayerProvider,
                                     BlockStateProvider middleLayerProvider,
                                     BlockStateProvider outerLayerProvider,
                                     List<BlockState> innerPlacements, TagKey<Block> cannotReplace,
                                     TagKey<Block> invalidBlocks) {

        public GeodeBlockSettings(BlockStateProvider fillingProvider,
                                  BlockStateProvider innerLayerProvider,
                                  BlockStateProvider alternateInnerLayerProvider,
                                  BlockStateProvider middleLayerProvider,
                                  BlockStateProvider outerLayerProvider,
                                  List<BlockState> innerPlacements, TagKey<Block> cannotReplace,
                                  TagKey<Block> invalidBlocks) {
            RandomSource source = new LegacyRandomSource(0);

            GTOreVeins.addVeinOre(fillingProvider.getState(source, BlockPos.ZERO).getBlock());
            GTOreVeins.addVeinOre(innerLayerProvider.getState(source, BlockPos.ZERO).getBlock());
            GTOreVeins.addVeinOre(alternateInnerLayerProvider.getState(source, BlockPos.ZERO).getBlock());
            GTOreVeins.addVeinOre(middleLayerProvider.getState(source, BlockPos.ZERO).getBlock());
            GTOreVeins.addVeinOre(outerLayerProvider.getState(source, BlockPos.ZERO).getBlock());
            for (BlockState bState : innerPlacements) {
                GTOreVeins.addVeinOre(bState.getBlock());
            }
            this.fillingProvider = fillingProvider;
            this.innerLayerProvider = innerLayerProvider;
            this.alternateInnerLayerProvider = alternateInnerLayerProvider;
            this.middleLayerProvider = middleLayerProvider;
            this.outerLayerProvider = outerLayerProvider;
            this.innerPlacements = innerPlacements;
            this.cannotReplace = cannotReplace;
            this.invalidBlocks = invalidBlocks;
        }

        // spotless:off
        public static final Codec<GeodeBlockSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        BlockStateProvider.CODEC.fieldOf("filling_provider").forGetter(config -> config.fillingProvider),
                        BlockStateProvider.CODEC.fieldOf("inner_layer_provider").forGetter(config -> config.innerLayerProvider),
                        BlockStateProvider.CODEC.fieldOf("alternate_inner_layer_provider").forGetter(config -> config.alternateInnerLayerProvider),
                        BlockStateProvider.CODEC.fieldOf("middle_layer_provider").forGetter(config -> config.middleLayerProvider),
                        BlockStateProvider.CODEC.fieldOf("outer_layer_provider").forGetter(config -> config.outerLayerProvider),
                        ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("inner_placements").forGetter(config -> config.innerPlacements),
                        TagKey.hashedCodec(Registries.BLOCK).fieldOf("cannot_replace").forGetter(config -> config.cannotReplace),
                        TagKey.hashedCodec(Registries.BLOCK).fieldOf("invalid_blocks").forGetter(config -> config.invalidBlocks)
                      )
                .apply(instance, GeodeBlockSettings::new));
        // spotless:on
    }
}
