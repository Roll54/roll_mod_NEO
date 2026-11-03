package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GTMOGS puts a vein's surface samples at {@code WORLD_SURFACE_WG}: the first air above anything at all.
 * Water, leaves, logs and plants all count, so samples ended up floating on lakes and the sea, on tree
 * tops and on top of tall grass instead of on the ground over the vein.
 *
 * <p>Here the height goes down from there to the first block with a solid top that is not part of a
 * tree, and the sample sits on it, in water if there is water (it is then placed waterlogged by
 * {@link SurfaceIndicatorWaterlogMixin}). Our samples are waterloggable, so flowing water does not
 * wash them away and a bucket fills them like a fence.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.api.worldgen.generator.indicators.SurfaceIndicatorGenerator")
public abstract class SurfaceIndicatorGroundMixin {

    @WrapOperation(method = "generate",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/WorldGenLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int roll_mod$groundHeight(WorldGenLevel level, Heightmap.Types type, int x, int z, Operation<Integer> original) {
        int top = original.call(level, type, x, z);
        int bottom = level.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top - 1, z);
        while (pos.getY() > bottom) {
            BlockState state = level.getBlockState(pos);
            if (roll_mod$isGround(level, pos, state)) return pos.getY() + 1;
            pos.move(Direction.DOWN);
        }
        return top;
    }

    /** A block a sample can stand on: a solid top face, and not a tree, a plant or a fluid. */
    @Unique
    private static boolean roll_mod$isGround(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced()) return false;
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) return false;
        return state.isFaceSturdy(level, pos, Direction.UP);
    }
}
