package com.roll_54.roll_mod.items.electricItems.orescanner;

import com.roll_54.roll_mod.compat.gtmogs.OreVeinLookups;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Counts roll_mod's ore materials per block column, every height, in a square of chunks around a player.
 *
 * <p>In two steps, so the counting stays off the server thread: {@link #snapshot} runs on it and copies just the
 * chunk sections whose palette can hold ore (live sections may change under another thread, and most sections are
 * skipped); {@link Snapshot#count} then runs anywhere. The world itself is read from memory, not the region files:
 * loaded chunks are newer than what is saved, and the files belong to the server's own I/O worker.
 */
public final class OreScan {

    private static final int[] EMPTY = new int[0];

    private OreScan() {}

    /** Server thread: the ore-bearing sections of every chunk in the square, copied. */
    public static Snapshot snapshot(ServerLevel level, BlockPos player, int radius) {
        ChunkPos center = new ChunkPos(player);
        // Per scan, so the shared block → material table is consulted once per block type, not once per block.
        Map<Block, Optional<OreDefinition>> materials = new IdentityHashMap<>();
        List<List<PalettedContainer<BlockState>>> chunks = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                ChunkAccess chunk = level.getChunk(center.x + dx, center.z + dz);
                List<PalettedContainer<BlockState>> sections = new ArrayList<>();
                for (LevelChunkSection section : chunk.getSections()) {
                    if (section.hasOnlyAir()) continue;
                    PalettedContainer<BlockState> states = section.getStates();
                    // The palette says whether any ore can be in here at all; most sections have none.
                    if (states.maybeHas(state -> material(state, materials).isPresent())) sections.add(states.copy());
                }
                chunks.add(sections);
            }
        }
        return new Snapshot(radius, center.x, center.z, player.getX(), player.getZ(), chunks, materials);
    }

    /** Copied sections, chunk by chunk; nothing in here is shared with the live world. */
    public record Snapshot(int radius, int centerX, int centerZ, int playerX, int playerZ,
                           List<List<PalettedContainer<BlockState>>> chunks,
                           Map<Block, Optional<OreDefinition>> materials) {

        /** Any thread: the per-column counts. */
        public OreScanResult count() {
            List<String> palette = new ArrayList<>();
            Map<String, Integer> paletteIndex = new HashMap<>();
            List<OreScanResult.Chunk> result = new ArrayList<>();
            for (List<PalettedContainer<BlockState>> sections : chunks) {
                result.add(OreScanResult.Chunk.of(columns(sections, materials, palette, paletteIndex), palette));
            }
            return new OreScanResult(radius, centerX, centerZ, playerX, playerZ, List.copyOf(palette), result);
        }
    }

    private static int[][] columns(List<PalettedContainer<BlockState>> sections,
                                   Map<Block, Optional<OreDefinition>> materials,
                                   List<String> palette, Map<String, Integer> paletteIndex) {
        @SuppressWarnings("unchecked")
        Map<Integer, Integer>[] counts = new Map[256];
        for (PalettedContainer<BlockState> states : sections) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Optional<OreDefinition> def = material(states.get(x, y, z), materials);
                        if (def.isEmpty()) continue;
                        int index = paletteIndex.computeIfAbsent(def.get().id(), id -> {
                            palette.add(id);
                            return palette.size() - 1;
                        });
                        int column = z * 16 + x;
                        if (counts[column] == null) counts[column] = new HashMap<>(4);
                        counts[column].merge(index, 1, Integer::sum);
                    }
                }
            }
        }
        int[][] columns = new int[256][];
        for (int i = 0; i < 256; i++) {
            if (counts[i] == null) {
                columns[i] = EMPTY;
                continue;
            }
            List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(counts[i].entrySet());
            entries.sort(Map.Entry.<Integer, Integer>comparingByValue().reversed());
            int[] column = new int[entries.size() * 2];
            for (int j = 0; j < entries.size(); j++) {
                column[2 * j] = entries.get(j).getKey();
                column[2 * j + 1] = entries.get(j).getValue();
            }
            columns[i] = column;
        }
        return columns;
    }

    private static Optional<OreDefinition> material(BlockState state, Map<Block, Optional<OreDefinition>> materials) {
        return materials.computeIfAbsent(state.getBlock(), block -> Optional.ofNullable(OreVeinLookups.materialOf(block)));
    }
}
