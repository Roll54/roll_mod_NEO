package com.roll_54.roll_mod.block;

import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.init.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LatexDandelion extends CropBlock {
    public static final MapCodec<LatexDandelion> CODEC = simpleCodec(LatexDandelion::new);

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 6);

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),   // stage 0
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D),   // stage 1
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),   // stage 2
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D),  // stage 3
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D),  // stage 4
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D),  // stage 5
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D)   // stage 6 (mature)
    };

    public LatexDandelion(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<LatexDandelion> codec() {
        return CODEC;
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 6;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ItemRegistry.LATEX_DANDELION_SEED.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[this.getAge(state)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}