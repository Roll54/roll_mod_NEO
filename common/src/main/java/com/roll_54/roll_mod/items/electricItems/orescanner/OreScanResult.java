package com.roll_54.roll_mod.items.electricItems.orescanner;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What an ore scanner found in the {@code (2 * radius + 1)²} chunks around {@code (centerX, centerZ)}, block column by
 * block column: for each column, how many blocks of each roll_mod ore material it holds and between which heights. Materials are
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

    /** Ints per material in a column: palette index, count, lowest Y, highest Y. */
    public static final int STRIDE = 4;

    /**
     * One chunk's ore. {@code columns[z * 16 + x]} holds {@link #STRIDE} ints per material {@code (palette index,
     * count, min Y, max Y)}, most first; an empty array for a column without ore. {@link #totals} and {@link #ranges}
     * ({@code {min Y, max Y}} per material over the whole chunk) are derived from the columns.
     */
    public record Chunk(int[][] columns, Map<String, Integer> totals, Map<String, int[]> ranges) {

        public static Chunk of(int[][] columns, List<String> palette) {
            Map<Integer, Integer> byIndex = new LinkedHashMap<>();
            Map<Integer, int[]> rangeByIndex = new HashMap<>();
            for (int[] column : columns) {
                for (int i = 0; i < column.length; i += STRIDE) {
                    byIndex.merge(column[i], column[i + 1], Integer::sum);
                    int[] range = rangeByIndex.computeIfAbsent(column[i],
                            k -> new int[] { Integer.MAX_VALUE, Integer.MIN_VALUE });
                    range[0] = Math.min(range[0], column[i + 2]);
                    range[1] = Math.max(range[1], column[i + 3]);
                }
            }
            Map<String, Integer> totals = new LinkedHashMap<>();
            Map<String, int[]> ranges = new HashMap<>();
            byIndex.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                    .forEach(e -> {
                        totals.put(palette.get(e.getKey()), e.getValue());
                        ranges.put(palette.get(e.getKey()), rangeByIndex.get(e.getKey()));
                    });
            return new Chunk(columns, totals, ranges);
        }

        public int[] column(int x, int z) {
            return columns[z * 16 + x];
        }
    }
}
