package com.roll_54.roll_mod.compat.MBD2.greenhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the greenhouse's blocks are, worked out from the controller: a 3×3×3 behind and above it, the
 * controller in the middle of the front face. The same cells {@code GreenhouseMachines#pattern} checks.
 */
public final class GreenhouseStructure {

    private GreenhouseStructure() {}

    /** The air cell the crop grows in: one block behind the controller, one up. */
    public static BlockPos cropCell(BlockPos controller, Direction front) {
        return controller.relative(front.getOpposite()).above();
    }

    /** Every block of the structure except the controller and the crop cell. */
    public static List<BlockPos> blocks(BlockPos controller, Direction front) {
        Direction back = front.getOpposite();
        Direction right = front.getClockWise();
        BlockPos crop = cropCell(controller, front);
        List<BlockPos> blocks = new ArrayList<>(25);
        for (int depth = 0; depth < 3; depth++) {
            for (int side = -1; side <= 1; side++) {
                for (int up = 0; up < 3; up++) {
                    BlockPos pos = controller.relative(back, depth).relative(right, side).above(up);
                    if (!pos.equals(controller) && !pos.equals(crop)) blocks.add(pos);
                }
            }
        }
        return blocks;
    }
}
