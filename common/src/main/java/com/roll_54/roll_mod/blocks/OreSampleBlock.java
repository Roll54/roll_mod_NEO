package com.roll_54.roll_mod.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Small ore outcrop that can sit on a floor or hang from a ceiling. Only the two vertical
 * orientations are valid: {@code vertical_direction=up} is the floor variant, {@code down} the
 * ceiling one (the model is flipped for it). Purely decorative geometry, so it has no collision.
 */
public class OreSampleBlock extends Block {

    public static final MapCodec<OreSampleBlock> CODEC = simpleCodec(OreSampleBlock::new);

    /** Vanilla's dripstone property: restricted to UP/DOWN, so no horizontal state can exist. */
    public static final DirectionProperty VERTICAL_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;

    private static final VoxelShape SHAPE_UP = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 4.0D, 12.0D);
    private static final VoxelShape SHAPE_DOWN = Block.box(4.0D, 12.0D, 4.0D, 12.0D, 16.0D, 12.0D);

    public OreSampleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VERTICAL_DIRECTION, Direction.UP));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VERTICAL_DIRECTION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Clicking the underside of a block hangs the sample from it; anything else lays it on the floor.
        Direction direction = context.getClickedFace() == Direction.DOWN ? Direction.DOWN : Direction.UP;
        return this.defaultBlockState().setValue(VERTICAL_DIRECTION, direction);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(VERTICAL_DIRECTION) == Direction.DOWN ? SHAPE_DOWN : SHAPE_UP;
    }
}
