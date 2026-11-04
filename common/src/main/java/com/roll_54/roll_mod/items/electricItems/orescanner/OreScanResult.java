package com.roll_54.roll_mod.items.electricItems.orescanner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What an ore scanner found in the {@code (2 * radius + 1)²} chunks around {@code (centerX, centerZ)}, block column by
 * block column: for each column, how many blocks of each roll_mod ore material it holds at any height. Materials are
 * stored as indices into {@code palette} ({@code OreDefinition} ids). Chunks are row-major, north to south and west
 * to east, so the player's chunk is the middle one. {@code playerX}/{@code playerZ} is the block the player stood on.
 */
public record OreScanResult(int radius, int centerX, int centerZ, int playerX, int playerZ, List<String> palette,
                            List<Chunk> chunks) {

    public int size() {
        return 2 * radius + 1;
    }

    /** The chunk at grid column {@code col} and row {@code row}. */
    public Chunk at(int col, int row) {
        return chunks.get(row * size() + col);
    }

    /**
     * One chunk's ore. {@code columns[z * 16 + x]} holds pairs {@code (palette index, count)}, most first; an empty
     * array for a column without ore. {@link #totals} is derived from the columns.
     */
    public record Chunk(int[][] columns, Map<String, Integer> totals) {

        public static Chunk of(int[][] columns, List<String> palette) {
            Map<Integer, Integer> byIndex = new LinkedHashMap<>();
            for (int[] column : columns) {
                for (int i = 0; i < column.length; i += 2) byIndex.merge(column[i], column[i + 1], Integer::sum);
            }
            Map<String, Integer> totals = new LinkedHashMap<>();
            byIndex.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                    .forEach(e -> totals.put(palette.get(e.getKey()), e.getValue()));
            return new Chunk(columns, totals);
        }

        public int[] column(int x, int z) {
            return columns[z * 16 + x];
        }
    }
}
