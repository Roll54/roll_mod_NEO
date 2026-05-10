package com.roll_54.roll_mod.mixin.antidupe;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;


@Mixin(PistonBaseBlock.class)
public abstract class PistonBlockMixin {

    @Inject(
            method = "moveBlocks",
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"
                    )
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void setAllToBeMovedBlockToAirFirst(
            Level level, BlockPos pos, Direction dir, boolean extending,
            CallbackInfoReturnable<Boolean> cir,
            @Local Map<BlockPos, BlockState> map,
            @Local(ordinal = 0) List<BlockPos> list,  // pistonHandler.getToPush() / getMovedBlocks()
            @Local(ordinal = 1) List<BlockState> list2  // states of list
    ) {

        for (int l = list.size() - 1; l >= 0; --l) {
            BlockPos toBeMovedBlockPos = list.get(l);
            BlockState toBeMovedBlockState = level.getBlockState(toBeMovedBlockPos);

            level.setBlock(toBeMovedBlockPos, Blocks.AIR.defaultBlockState(), 2 | 4 | 16 | 64);
            list2.set(l, toBeMovedBlockState);
            map.put(toBeMovedBlockPos, toBeMovedBlockState);
        }
    }

    @Inject(
            method = "moveBlocks",
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;isSticky:Z"
                    )
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;keySet()Ljava/util/Set;",
                    ordinal = 0
            )
    )
    private void makeSureStatesInBlockStatesIsCorrect(
            Level level, BlockPos pos, Direction dir, boolean extending,
            CallbackInfoReturnable<Boolean> cir,
            @Local(ordinal = 0) List<BlockPos> list,  // pistonHandler.getToPush()
            @Local(ordinal = 1) List<BlockState> list2,  // states of list
            @Local(ordinal = 2) List<BlockPos> list3,  // pistonHandler.getToDestroy()
            @Local BlockState[] blockStates,
            @Local(ordinal = 0) int j
    ) {

        int j2 = list3.size();
        for (int l2 = list.size() - 1; l2 >= 0; --l2) {
            blockStates[j2++] = list2.get(l2);
        }
    }
}