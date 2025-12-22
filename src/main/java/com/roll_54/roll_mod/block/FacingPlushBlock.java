package com.roll_54.roll_mod.block;

import com.roll_54.roll_mod.init.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import static com.roll_54.roll_mod.init.BlockRegistry.ROLL_PLUSH;

public class FacingPlushBlock extends Block {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

    public FacingPlushBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }
//todo sound effect when on click

//    @Override
//    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//                                 InteractionHand hand, BlockHitResult hit) {
//        if (!level.isClientSide) {
//            if (state.getBlock() == ROLL_PLUSH.get()) {
//                level.playSound(
//                        null, pos,
//                        SoundRegistry.ROLL_CHIPUNK.get(),
//                        SoundSource.BLOCKS,
//                        1.0F, 1.0F
//                );
//            }
//        }
//        return InteractionResult.SUCCESS;
//    }

}
