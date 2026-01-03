package com.roll_54.roll_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class RegenBlock extends Block {

    private final BlockState depletedState;

    public RegenBlock(Properties properties, Block depletedBlock) {
        super(properties);
        this.depletedState = depletedBlock.defaultBlockState();
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            FluidState fluid
    ) {
        if (!level.isClientSide) {
            dropResources(state, level, pos, null, player, player.getMainHandItem());

            level.setBlock(pos, depletedState, Block.UPDATE_ALL);
            level.scheduleTick(pos, depletedState.getBlock(), 20 * 60);
        }
        return false;
    }
}
