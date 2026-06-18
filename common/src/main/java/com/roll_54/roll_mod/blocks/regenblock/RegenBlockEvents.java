package com.roll_54.roll_mod.blocks.regenblock;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks creative mode breaks of RegenBlocks.
 * This class helps manage the state of block breaks in creative mode.
 */
public class RegenBlockEvents {
    private static final Map<BlockPos, Boolean> CREATIVE_BREAKS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> BREAK_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final long BREAK_TIMEOUT = 1000; // 1 second timeout for creative breaks

    public static void markCreativeBreak(BlockPos pos) {
        CREATIVE_BREAKS.put(pos, Boolean.TRUE);
        BREAK_TIMESTAMPS.put(pos, System.currentTimeMillis());
    }

    public static boolean consumeCreativeBreak(BlockPos pos) {
        Long timestamp = BREAK_TIMESTAMPS.get(pos);

        // Clean up old entries to prevent memory leaks
        if (timestamp != null && System.currentTimeMillis() - timestamp > BREAK_TIMEOUT) {
            CREATIVE_BREAKS.remove(pos);
            BREAK_TIMESTAMPS.remove(pos);
            return false;
        }

        boolean wasCreative = CREATIVE_BREAKS.remove(pos) != null;
        if (wasCreative) {
            BREAK_TIMESTAMPS.remove(pos);
        }
        return wasCreative;
    }

    public static boolean isCreativeBreakMarked(BlockPos pos) {
        Long timestamp = BREAK_TIMESTAMPS.get(pos);

        // Clean up old entries
        if (timestamp != null && System.currentTimeMillis() - timestamp > BREAK_TIMEOUT) {
            CREATIVE_BREAKS.remove(pos);
            BREAK_TIMESTAMPS.remove(pos);
            return false;
        }

        return CREATIVE_BREAKS.containsKey(pos);
    }
}