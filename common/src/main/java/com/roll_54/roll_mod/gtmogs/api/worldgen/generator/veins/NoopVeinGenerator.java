package com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreBlockPlacer;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class NoopVeinGenerator extends VeinGenerator {

    public static final NoopVeinGenerator INSTANCE = new NoopVeinGenerator();
    public static final MapCodec<NoopVeinGenerator> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    public List<VeinEntry> getAllEntries() {
        return Collections.emptyList();
    }

    @Override
    public Map<BlockPos, OreBlockPlacer> generate(WorldGenLevel level, RandomSource random, OreVeinDefinition entry,
                                                  BlockPos origin) {
        return Collections.emptyMap();
    }

    @Override
    public VeinGenerator build() {
        return this;
    }

    @Override
    public VeinGenerator copy() {
        return INSTANCE;
    }

    @Override
    public MapCodec<? extends VeinGenerator> codec() {
        return CODEC;
    }
}
