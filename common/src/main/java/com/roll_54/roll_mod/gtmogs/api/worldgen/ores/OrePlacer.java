package com.roll_54.roll_mod.gtmogs.api.worldgen.ores;
import com.roll_54.roll_mod.worldgen.MiningWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Responsible for placing ores of surrounding veins for the current chunk.
 * 
 * <p>
 * Surrounding veins are resolved from the {@link OreGenCache} and placed using each block position's
 * {@link OreBlockPlacer}.
 */
public class OrePlacer {

    @Getter
    private final OreGenCache oreGenCache = new OreGenCache();

    /**
     * Place the contents of all surrounding ore veins in the current chunk.
     * 
     * <p>
     * Consumes the current chunk for all of the relevant veins, allowing the cache to unload the vein,
     * once all of its chunks have been generated.
     */
    /**
     * roll_mod: veins and their samples go into flat worlds only with the developer option
     * {@code doSuperflatOres}, and always into the mining world ({@code roll_mod:mining_overworld}), which is
     * flat. Other flat worlds stay empty.
     */
    private static boolean superflatOres(WorldGenLevel level) {
        return ConfigHolder.INSTANCE.dev.doSuperflatOres || level.getLevel().dimension() == MiningWorld.LEVEL;
    }

    public void placeOres(WorldGenLevel level, ChunkGenerator chunkGenerator, ChunkAccess chunk) {
        if (!superflatOres(level) && chunkGenerator instanceof FlatLevelSource) return;

        long started = OreGenDiagnostics.start();
        var random = new XoroshiroRandomSource(level.getSeed() ^ chunk.getPos().toLong());
        var generatedVeins = oreGenCache.consumeChunkVeins(level, chunkGenerator, chunk);

        try (BulkSectionAccess access = new BulkSectionAccess(level)) {
            generatedVeins.forEach(generatedVein -> placeVein(chunk.getPos(), random, access, generatedVein, null));
        }

        OreGenDiagnostics.recordChunk(OreGenDiagnostics.Phase.PLACE_ORES, started, level, chunk.getPos(),
                generatedVeins.size());
    }

    /**
     * Place the indicators of all surrounding ore veins in the current chunk.
     *
     * <p>
     * Indicators bypass the layer's stone {@link RuleTest} (via {@link AlwaysTrueTest}); each indicator placer
     * verifies the target block is air/replaceable before writing, so existing blocks are never overwritten.
     *
     * <p>
     * Bailing out on the config check before touching the {@link OreGenCache} skips indicator generation and
     * caching too, not just the placement, since this is the cache's only consumer.
     */
    public void placeIndicators(WorldGenLevel level, ChunkGenerator chunkGenerator, ChunkAccess chunk) {
        if (!ConfigHolder.INSTANCE.worldgen.oreVeins.generateOreIndicators) return;
        if (!superflatOres(level) && chunkGenerator instanceof FlatLevelSource) return;

        long started = OreGenDiagnostics.start();
        var random = new XoroshiroRandomSource(level.getSeed() ^ chunk.getPos().toLong());
        var generatedIndicators = oreGenCache.consumeChunkIndicators(level, chunkGenerator, chunk);

        try (BulkSectionAccess access = new BulkSectionAccess(level)) {
            generatedIndicators.forEach(
                    indicator -> placeVein(chunk.getPos(), random, access, indicator, AlwaysTrueTest.INSTANCE));
        }

        OreGenDiagnostics.recordChunk(OreGenDiagnostics.Phase.PLACE_INDICATORS, started, level, chunk.getPos(),
                generatedIndicators.size());
    }

    public void placeVein(ChunkPos chunk, RandomSource random, BulkSectionAccess access,
                          GeneratedVein generatedVein, @Nullable RuleTest targetOverride) {
        Map<BlockPos, OreBlockPlacer> ores = generatedVein.consumeOres(chunk);

        // Most of the veins resolved for a chunk reach into it without actually covering it. Bailing out here
        // skips the grouping collector for those, which previously allocated two maps per vein to describe an
        // empty result. No randomness is consumed on this path either way, since the loop below never ran.
        if (ores.isEmpty())
            return;

        RuleTest layerTarget = targetOverride != null ? targetOverride : generatedVein.getLayer().getTarget();

        resolvePlacerLists(ores).forEach(((sectionPos, placers) -> {
            LevelChunkSection section = access.getSection(sectionPos.origin());

            if (section == null)
                return;

            placers.forEach((pos, placer) -> {
                var blockState = section.getBlockState(
                        SectionPos.sectionRelative(pos.getX()),
                        SectionPos.sectionRelative(pos.getY()),
                        SectionPos.sectionRelative(pos.getZ()));

                if (layerTarget.test(blockState, random))
                    placer.placeBlock(access, section);
            });
        }));
    }

    private Map<SectionPos, Map<BlockPos, OreBlockPlacer>> resolvePlacerLists(Map<BlockPos, OreBlockPlacer> ores) {
        return ores.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> SectionPos.of(entry.getKey()),
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
