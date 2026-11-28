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
        List<List<Section>> chunks = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                ChunkAccess chunk = level.getChunk(center.x + dx, center.z + dz);
                List<Section> sections = new ArrayList<>();
                LevelChunkSection[] all = chunk.getSections();
                for (int i = 0; i < all.length; i++) {
                    LevelChunkSection section = all[i];
                    if (section.hasOnlyAir()) continue;
                    PalettedContainer<BlockState> states = section.getStates();
                    // The palette says whether any ore can be in here at all; most sections have none.
                    if (states.maybeHas(state -> material(state, materials).isPresent())) {
                        sections.add(new Section(chunk.getSectionYFromSectionIndex(i) << 4, states.copy()));
                    }
                }
                chunks.add(sections);
            }
        }
        return new Snapshot(radius, center.x, center.z, player.getX(), player.getZ(), chunks, materials);
    }

    /** One copied section and the Y of its lowest block. */
    public record Section(int bottomY, PalettedContainer<BlockState> states) {}

    /** Copied sections, chunk by chunk; nothing in here is shared with the live world. */
    public record Snapshot(int radius, int centerX, int centerZ, int playerX, int playerZ,
                           List<List<Section>> chunks,
                           Map<Block, Optional<OreDefinition>> materials) {

        /** Any thread: the per-column counts. */
        public OreScanResult count() {
            List<String> palette = new ArrayList<>();
            Map<String, Integer> paletteIndex = new HashMap<>();
            List<OreScanResult.Chunk> result = new ArrayList<>();
            for (List<Section> sections : chunks) {
                result.add(OreScanResult.Chunk.of(columns(sections, materials, palette, paletteIndex), palette));
            }
            return new OreScanResult(radius, centerX, centerZ, playerX, playerZ, List.copyOf(palette), result);
        }
    }

    private static int[][] columns(List<Section> sections,
                                   Map<Block, Optional<OreDefinition>> materials,
                                   List<String> palette, Map<String, Integer> paletteIndex) {
        @SuppressWarnings("unchecked")
        Map<Integer, int[]>[] counts = new Map[256]; // palette index -> {count, min Y, max Y}
        for (Section section : sections) {
            PalettedContainer<BlockState> states = section.states();
            for (int y = 0; y < 16; y++) {
                int worldY = section.bottomY() + y;
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
                        int[] c = counts[column].computeIfAbsent(index,
                                k -> new int[] { 0, Integer.MAX_VALUE, Integer.MIN_VALUE });
                        c[0]++;
                        c[1] = Math.min(c[1], worldY);
                        c[2] = Math.max(c[2], worldY);
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
            List<Map.Entry<Integer, int[]>> entries = new ArrayList<>(counts[i].entrySet());
            entries.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
            int stride = OreScanResult.STRIDE;
            int[] column = new int[entries.size() * stride];
            for (int j = 0; j < entries.size(); j++) {
                int[] c = entries.get(j).getValue();
                column[stride * j] = entries.get(j).getKey();
                column[stride * j + 1] = c[0];
                column[stride * j + 2] = c[1];
                column[stride * j + 3] = c[2];
            }
            columns[i] = column;
        }
        return columns;
    }

    private static Optional<OreDefinition> material(BlockState state, Map<Block, Optional<OreDefinition>> materials) {
        return materials.computeIfAbsent(state.getBlock(), block -> Optional.ofNullable(OreVeinLookups.materialOf(block)));
    }
}
