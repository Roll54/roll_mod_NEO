package com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins;

import com.roll_54.roll_mod.compat.gtmogs.OreStoneVariants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreBlockPlacer;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreVeinUtil;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;
import com.roll_54.roll_mod.gtmogs.utils.GTUtil;
import com.roll_54.roll_mod.gtmogs.utils.WeightedEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Accessors(fluent = true, chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class DikeVeinGenerator extends VeinGenerator {

    public static final MapCodec<DikeVeinGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.list(DikeBlockDefinition.CODEC).fieldOf("blocks").forGetter(it -> it.blocks),
            Codec.INT.fieldOf("min_y").forGetter(it -> it.minYLevel),
            Codec.INT.fieldOf("max_y").forGetter(it -> it.maxYLevel)).apply(instance, DikeVeinGenerator::new));

    public List<DikeBlockDefinition> blocks;
    @Setter
    public int minYLevel;
    @Setter
    public int maxYLevel;

    @Override
    public List<VeinEntry> getAllEntries() {
        List<VeinEntry> entries = new ArrayList<>(this.blocks.size());
        for (var def : this.blocks) {
            VeinGenerator.mapTarget(def.block, def.weight).forEach(entries::add);
        }
        return entries;
    }

    @Override
    public Map<BlockPos, OreBlockPlacer> generate(WorldGenLevel level, RandomSource random, OreVeinDefinition entry,
                                                  BlockPos origin) {
        Map<BlockPos, OreBlockPlacer> generatedBlocks = new Object2ObjectOpenHashMap<>();

        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(level.getSeed()));
        NormalNoise normalNoise = NormalNoise.create(worldgenRandom, -2, 4.0D);
        ChunkPos chunkPos = new ChunkPos(origin);

        float density = entry.density();
        int size = entry.clusterSize().sample(random);

        int radius = Mth.ceil(size / 2f);

        int xPos = chunkPos.getMinBlockX() + level.getRandom().nextInt(16);
        int zPos = chunkPos.getMinBlockZ() + level.getRandom().nextInt(16);

        int yTop = maxYLevel;
        int yBottom = minYLevel;

        BlockPos basePos = new BlockPos(xPos, yBottom, zPos);

        for (int dY = yBottom; dY <= yTop; dY++) {
            for (int dX = -radius; dX <= radius; dX++) {
                for (int dZ = -radius; dZ <= radius; dZ++) {
                    float dist = (dX * dX) + (dZ * dZ);
                    if (dist > radius * 2) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(basePos.getX() + dX, dY, basePos.getZ() + dZ);
                    if (normalNoise.getValue(dX, dY, dZ) >= 0.5 && random.nextFloat() <= density) {
                        final var randomSeed = random.nextLong(); // Fully deterministic regardless of chunk order

                        generatedBlocks.put(pos,
                                (access, section) -> placeBlock(access, section, randomSeed, pos, entry));
                    }
                }
            }
        }

        return generatedBlocks;
    }

    private void placeBlock(
                            BulkSectionAccess level, LevelChunkSection section, long randomSeed, BlockPos pos,
                            OreVeinDefinition entry) {
        var rand = new XoroshiroRandomSource(randomSeed);
        DikeBlockDefinition blockDefinition = GTUtil.getRandomItem(rand, blocks);
        BlockState current = level.getBlockState(pos);

        int x = SectionPos.sectionRelative(pos.getX());
        int y = SectionPos.sectionRelative(pos.getY());
        int z = SectionPos.sectionRelative(pos.getZ());

        if (pos.getY() >= blockDefinition.minY() && pos.getY() <= blockDefinition.maxY()) {

            for (TargetBlockState targetState : blockDefinition.block) {
                if (!OreVeinUtil.canPlaceOre(current, level::getBlockState, rand, entry, targetState,
                        pos.mutable()))
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

    @Override
    public VeinGenerator build() {
        return this;
    }

    @Override
    public VeinGenerator copy() {
        return new DikeVeinGenerator(new ArrayList<>(blocks), minYLevel, maxYLevel);
    }

    @Override
    public MapCodec<? extends VeinGenerator> codec() {
        return CODEC;
    }

    public DikeVeinGenerator withBlock(BlockState blockState, int weight, int minY, int maxY) {
        GTOreVeins.addVeinOre(blockState.getBlock());
        TargetBlockState target = OreConfiguration.target(AlwaysTrueTest.INSTANCE, blockState);
        return this.withBlock(new DikeBlockDefinition(List.of(target), weight, minY, maxY));
    }

    public DikeVeinGenerator withBlock(DikeBlockDefinition block) {
        // actually hilariously bad
        if (this.blocks == null) this.blocks = new ArrayList<>();
        var defs = blocks.stream().map(def -> def.block);
        var blocks = defs.map(target -> target.stream().map(tbs -> tbs.state.getBlock()));
        blocks.forEach(s -> s.forEach(GTOreVeins::addVeinOre));
        this.blocks.add(block);
        return this;
    }

    public record DikeBlockDefinition(List<TargetBlockState> block, int weight, int minY, int maxY)
            implements WeightedEntry {

        // spotless:off
        public static final Codec<DikeBlockDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        TargetBlockState.CODEC.listOf().fieldOf("block").forGetter(x -> x.block),
                        Codec.INT.fieldOf("weight").forGetter(x -> x.weight),
                        Codec.INT.fieldOf("min_y").orElse(320).forGetter(x -> x.minY),
                        Codec.INT.fieldOf("max_y").orElse(-64).forGetter(x -> x.maxY))
                .apply(instance, DikeBlockDefinition::new));

    }
}
