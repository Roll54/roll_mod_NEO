package com.roll_54.roll_mod.mixin.antidupe;

import com.google.common.base.MoreObjects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TripWireHookBlock.class)
public abstract class TripwireHookBlockMixin extends Block {

    public TripwireHookBlockMixin(Properties properties) {
        super(properties);
    }

    @Shadow
    private static void emitState(Level level, BlockPos pos, boolean attached, boolean on, boolean detached, boolean off) { }

    @Shadow
    private static void notifyNeighbors(Block block, Level level, BlockPos pos, Direction direction) { }

    @Inject(
            method = "calculateState(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZZILnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void updateMixin(Level level, BlockPos pos, BlockState state, boolean beingRemoved,
                                    boolean bl, int i, @Nullable BlockState blockState, CallbackInfo ci) {

        Direction direction = state.getValue(TripWireHookBlock.FACING);
        boolean attached = state.getValue(TripWireHookBlock.ATTACHED);
        boolean powered = state.getValue(TripWireHookBlock.POWERED);
        Block block = state.getBlock();
        boolean notRemoving = !beingRemoved;
        boolean on = false;
        int index = 0;
        BlockState[] blockStates = new BlockState[42];
        BlockPos blockPos;

        for (int k = 1; k < 42; ++k) {
            blockPos = pos.relative(direction, k);
            BlockState blockState2 = level.getBlockState(blockPos);
            if (blockState2.is(Blocks.TRIPWIRE_HOOK)) {
                if (blockState2.getValue(TripWireHookBlock.FACING) == direction.getOpposite()) {
                    index = k;
                }
                break;
            }
            if (!blockState2.is(Blocks.TRIPWIRE) && k != i) {
                blockStates[k] = null;
                notRemoving = false;
            } else {
                if (k == i) {
                    blockState2 = MoreObjects.firstNonNull(blockState, blockState2);
                }
                boolean armed = !blockState2.getValue(TripWireBlock.DISARMED);
                on |= armed && blockState2.getValue(TripWireBlock.POWERED);
                blockStates[k] = blockState2;
                if (k == i) {
                    level.scheduleTick(pos, block, 10);
                    notRemoving &= armed;
                }
            }
        }

        notRemoving &= index > 1;
        on &= notRemoving;
        BlockState newState = block.defaultBlockState()
                .setValue(TripWireHookBlock.ATTACHED, notRemoving)
                .setValue(TripWireHookBlock.POWERED, on);

        if (index > 0) {
            blockPos = pos.relative(direction, index);
            Direction blockState2 = direction.getOpposite();
            level.setBlock(blockPos, newState.setValue(TripWireHookBlock.FACING, blockState2), 3);
            notifyNeighbors(block, level, blockPos, blockState2);
            emitState(level, blockPos, notRemoving, on, attached, powered);
        }

        emitState(level, pos, notRemoving, on, attached, powered);

        if (!beingRemoved && level.getBlockState(pos).is(Blocks.TRIPWIRE_HOOK)) {
            level.setBlock(pos, newState.setValue(TripWireHookBlock.FACING, direction), 3);
            if (bl) {
                notifyNeighbors(block, level, pos, direction);
            }
        }

        if (attached != notRemoving) {
            for (int x = 1; x < index; ++x) {
                BlockPos pos2 = pos.relative(direction, x);
                BlockState state2 = blockStates[x];
                if (state2 != null) {
                    if (level.getBlockState(pos2).is(Blocks.TRIPWIRE_HOOK)) {
                        level.setBlock(pos2, state2.setValue(TripWireHookBlock.ATTACHED, notRemoving), 3);
                    }
                }
            }
        }

        ci.cancel();
    }
}