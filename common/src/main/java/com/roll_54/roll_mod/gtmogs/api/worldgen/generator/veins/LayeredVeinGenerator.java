package com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins;

import com.roll_54.roll_mod.compat.gtmogs.OreStoneVariants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.gtmogs.api.worldgen.GTLayerPattern;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreBlockPlacer;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreVeinUtil;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedReturnValue")
@NoArgsConstructor
public class LayeredVeinGenerator extends VeinGenerator {

    // spotless:off
    public static final MapCodec<LayeredVeinGenerator> CODEC = GTLayerPattern.CODEC.listOf().fieldOf("layer_patterns")
            .xmap(LayeredVeinGenerator::new, LayeredVeinGenerator::getLayerPatterns);
    // spotless:on
    private final List<NonNullSupplier<GTLayerPattern>> bakingLayerPatterns = new ArrayList<>();

    public List<GTLayerPattern> layerPatterns;

    public List<GTLayerPattern> getLayerPatterns() {
        if (layerPatterns == null || this.layerPatterns.isEmpty()) {
            layerPatterns = bakingLayerPatterns.stream().map(Supplier::get).collect(Collectors.toList());
        }
        return layerPatterns;
    }

    @Override
    public List<VeinEntry> getAllEntries() {
        return getLayerPatterns().stream()
                .flatMap(pattern -> pattern.layers().stream())
                .flatMap(GTLayerPattern.Layer::asVeinEntries)
                .distinct()
                .toList();
    }

    @Override
    public Map<BlockPos, OreBlockPlacer> generate(WorldGenLevel level, RandomSource random, OreVeinDefinition entry,
                                                  BlockPos origin) {
        Map<BlockPos, OreBlockPlacer> generatedBlocks = new Object2ObjectOpenHashMap<>();
        var patternPool = this.getLayerPatterns();

        if (patternPool.isEmpty())
            return Map.of();

        GTLayerPattern layerPattern = patternPool.get(random.nextInt(patternPool.size()));

        int size = entry.clusterSize().sample(random);
        float density = entry.density();

        int radius = Mth.ceil(size / 2f);

        int xMin = origin.getX() - radius;
        int yMin = origin.getY() - radius;
        int zMin = origin.getZ() - radius;
        int width = (radius * 2) + 1;
        int length = (radius * 2) + 1;
        int height = (radius * 2) + 1;

        if (origin.getY() >= level.getMaxBuildHeight())
            return Map.of();

        List<GTLayerPattern.Layer> resolvedLayers = new ArrayList<>();
        FloatList layerDiameterOffsets = new FloatArrayList();

        int layerCoordinate = random.nextInt(4);
        int slantyCoordinate = random.nextInt(3);
        float slope = random.nextFloat() * .75f;

        for (int xOffset = 0; xOffset < width; xOffset++) {
            float sizeFractionX = xOffset * 2f / width - 1;
            if ((sizeFractionX * sizeFractionX) > 1)
                continue;

            for (int yOffset = 0; yOffset < height; yOffset++) {
                float sizeFractionY = yOffset * 2f / height - 1;
                if ((sizeFractionX * sizeFractionX) + (sizeFractionY * sizeFractionY) > 1)
                    continue;
                if (level.isOutsideBuildHeight(yMin + yOffset))
                    continue;

                for (int zOffset = 0; zOffset < length; zOffset++) {
                    float sizeFractionZ = zOffset * 2f / length - 1;

                    int layerIndex = layerCoordinate == 0 ? zOffset : layerCoordinate == 1 ? xOffset : yOffset;
                    if (slantyCoordinate != layerCoordinate)
                        layerIndex += Mth.floor(
                                slantyCoordinate == 0 ? zOffset : slantyCoordinate == 1 ? xOffset : yOffset * slope);

                    while (layerIndex >= resolvedLayers.size()) {
                        GTLayerPattern.Layer next = layerPattern.rollNext(
                                resolvedLayers.isEmpty() ? null : resolvedLayers.getLast(),
                                random);
                        float offset = random.nextFloat() * .5f + .5f;
                        for (int i = 0; i < next.minSize() + random.nextInt(1 + next.maxSize() - next.minSize()); i++) {
                            resolvedLayers.add(next);
                            layerDiameterOffsets.add(offset);
                        }
                    }

                    if ((sizeFractionX * sizeFractionX) + (sizeFractionY * sizeFractionY) +
                            (sizeFractionZ * sizeFractionZ) > 1 * layerDiameterOffsets.getFloat(layerIndex))
                        continue;

                    GTLayerPattern.Layer layer = resolvedLayers.get(layerIndex);
                    List<OreConfiguration.TargetBlockState> state = layer.rollBlock(random);

                    int currentX = xMin + xOffset;
                    int currentY = yMin + yOffset;
                    int currentZ = zMin + zOffset;

                    final var randomSeed = random.nextLong(); // Fully deterministic regardless of chunk order

                    BlockPos currentPos = new BlockPos(currentX, currentY, currentZ);
                    generatedBlocks.put(currentPos, (access, section) -> placeBlock(access, section, randomSeed, entry,
                            density, state, currentPos));
                }
            }
        }

        return generatedBlocks;
    }

    private static void placeBlock(BulkSectionAccess access, LevelChunkSection section, long randomSeed,
                                   OreVeinDefinition entry, float density,
                                   List<OreConfiguration.TargetBlockState> state, BlockPos pos) {
        RandomSource random = new XoroshiroRandomSource(randomSeed);
        int x = SectionPos.sectionRelative(pos.getX());
        int y = SectionPos.sectionRelative(pos.getY());
        int z = SectionPos.sectionRelative(pos.getZ());

        BlockState blockState = section.getBlockState(x, y, z);
        BlockPos.MutableBlockPos posCursor = pos.mutable();

        if (random.nextFloat() <= density) {

            for (OreConfiguration.TargetBlockState targetState : state) {
                if (!OreVeinUtil.canPlaceOre(blockState, access::getBlockState, random, entry, targetState,
                        posCursor))
                    continue;
                if (targetState.state.isAir())
                    continue;
                // roll_mod: the same ore in the rock it lands in (granite hematite in granite), see OreStoneVariants.
                section.setBlockState(x, y, z,
                        OreStoneVariants.forRock(targetState.state, section.getBlockState(x, y, z)), false);
                break;
            }

        }
    }

    public LayeredVeinGenerator(List<GTLayerPattern> layerPatterns) {
        super();
        for (var layerPattern : layerPatterns) {
            layerPattern.layers().forEach((layer) -> {
                layer.asVeinEntries().forEach((entry) -> {
                    GTOreVeins.addVeinOre(entry.vein().getBlock());
                });
            });
        }
        this.layerPatterns = layerPatterns;
    }

    public LayeredVeinGenerator withLayerPattern(NonNullSupplier<GTLayerPattern> pattern) {
        this.bakingLayerPatterns.add(pattern);
        return this;
    }

    public VeinGenerator build() {
        if (this.layerPatterns != null && !this.layerPatterns.isEmpty()) return this;
        this.layerPatterns = this.bakingLayerPatterns.stream()
                .map(NonNullSupplier::get)
                .toList();
        return this;
    }

    @Override
    public VeinGenerator copy() {
        return new LayeredVeinGenerator(new ArrayList<>(this.layerPatterns));
    }

    @Override
    public MapCodec<? extends VeinGenerator> codec() {
        return CODEC;
    }
}
