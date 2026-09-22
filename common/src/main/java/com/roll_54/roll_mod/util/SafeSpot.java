package com.roll_54.roll_mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Finding somewhere a player can be dropped without dying for it.
 *
 * <p>Grew out of the rocket's landing search and is now shared with {@code /rtp}: a solid floor, two
 * blocks of air above it, no lava or fire underfoot, and — in the Nether — not the bedrock roof.
 *
 * <p>Every method here reads blocks directly, so the chunk must already be loaded. Callers that pick
 * coordinates at random are the ones paying for that, and they load one column at a time rather than
 * a ring of them.
 */
public final class SafeSpot {

    /**
     * The Nether's bedrock roof sits around y=127. Build height is 320 in modern versions, so it
     * cannot be derived from the level — a floor at or above this is the roof, not the ground.
     */
    private static final int NETHER_ROOF_FLOOR_MIN_Y = 123;

    /** How much headroom a Nether landing needs to be sure it is not in a pocket under the roof. */
    private static final int NETHER_HEADROOM = 8;

    private SafeSpot() {}

    /**
     * Scans a square spiral of the given radius for a standable spot.
     *
     * @return where the player's feet go, or {@code null} when nothing in range qualifies.
     */
    public static @Nullable BlockPos around(ServerLevel level, int originX, int originZ, int radius) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Only the perimeter ring, or every column inside r would be tried r times.
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    BlockPos candidate = inColumn(level, originX + dx, originZ + dz);
                    if (candidate != null) return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Scans one column from the top down for a standable spot.
     *
     * @return where the player's feet go, or {@code null} when the column has nowhere safe.
     */
    public static @Nullable BlockPos inColumn(ServerLevel level, int x, int z) {
        return inColumnFrom(level, x, z, level.getMaxBuildHeight() - 2,
                level.getMaxBuildHeight() - level.getMinBuildHeight());
    }

    /**
     * Scans downwards from {@code startY} for at most {@code depth} blocks.
     *
     * <p>What a caller with a heightmap reading in hand wants: the surface is a few blocks below
     * where the generator says it is, and scanning the other 300 blocks of the column only finds
     * caves.
     *
     * @return where the player's feet go, or {@code null} when that stretch has nowhere safe.
     */
    public static @Nullable BlockPos inColumnFrom(ServerLevel level, int x, int z, int startY, int depth) {
        int top = Math.min(startY, level.getMaxBuildHeight() - 2);
        int bottom = Math.max(level.getMinBuildHeight() + 1, top - depth);
        for (int y = top; y >= bottom; y--) {
            BlockPos floor = new BlockPos(x, y, z);
            BlockPos stand = floor.above();
            BlockPos head = stand.above();

            BlockState floorState = level.getBlockState(floor);
            if (!floorState.isFaceSturdy(level, floor, Direction.UP)) continue;
            if (hurts(floorState)) continue;
            // Air rather than "not solid", so water and lava columns are rejected as well.
            if (!level.getBlockState(stand).isAir() || !level.getBlockState(head).isAir()) continue;

            if (level.dimension() == Level.NETHER && !safeInNether(level, stand)) continue;

            return stand;
        }
        return null;
    }

    /** Whether standing on this block is a slow way of dying. */
    private static boolean hurts(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(BlockTags.FIRE)
                || state.is(BlockTags.CAMPFIRES)
                || !state.getFluidState().isEmpty();
    }

    /** Rejects the roof band and any pocket without real headroom under it. */
    private static boolean safeInNether(ServerLevel level, BlockPos stand) {
        if (stand.below().getY() >= NETHER_ROOF_FLOOR_MIN_Y) return false;
        for (int i = 0; i <= NETHER_HEADROOM; i++) {
            if (!level.getBlockState(stand.above(i)).isAir()) return false;
        }
        return true;
    }
}
