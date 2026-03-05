package com.roll_54.roll_mod.blocks;

import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.blocks.entity.RocketControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.roll_54.roll_mod.init.BlockEntites.ROCKET_CONTROLLER_BE;

public class RocketControllerBlock extends BaseEntityBlock {
    public static final MapCodec<RocketControllerBlock> CODEC = simpleCodec(RocketControllerBlock::new);

    public static final DirectionProperty FACING    = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty  ADDITIONAL = BooleanProperty.create("additional");

    /** 8 horizontal offsets around the center (dx, dz) */
    private static final int[][] OFFSETS = {
        {-1, -1}, {0, -1}, {1, -1},
        {-1,  0},           {1,  0},
        {-1,  1}, {0,  1}, {1,  1}
    };

    public RocketControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ADDITIONAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ADDITIONAL);
    }

    // ── Placement ────────────────────────────────────────────────────

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos center = context.getClickedPos();
        Level level = context.getLevel();
        // All 8 surrounding positions must be replaceable
        for (int[] off : OFFSETS) {
            BlockPos p = center.offset(off[0], 0, off[1]);
            if (!level.getBlockState(p).canBeReplaced(context)) return null;
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(ADDITIONAL, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockState additionalState = state.setValue(ADDITIONAL, true);
        for (int[] off : OFFSETS) {
            level.setBlock(pos.offset(off[0], 0, off[1]), additionalState, Block.UPDATE_ALL);
        }
    }

    // ── Rotation / Mirror ─────────────────────────────────────────────

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ── Block Entity ──────────────────────────────────────────────────

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(ADDITIONAL)) return null;
        return new RocketControllerBlockEntity(pos, state);
    }

    // ── Rendering ────────────────────────────────────────────────────

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ── Interaction ───────────────────────────────────────────────────

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            RocketControllerBlockEntity mainBe = getMainBlockEntity(level, pos, state);
            if (mainBe != null) {
                player.openMenu(mainBe, mainBe.getBlockPos());
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ── Removal ───────────────────────────────────────────────────────

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            boolean isAdditional = pState.getValue(ADDITIONAL);

            if (isAdditional) {
                // Find and destroy the main block (search all 8 neighbors)
                for (int[] off : OFFSETS) {
                    BlockPos neighbor = pPos.offset(off[0], 0, off[1]);
                    BlockState neighborState = pLevel.getBlockState(neighbor);
                    if (neighborState.is(this) && !neighborState.getValue(ADDITIONAL)) {
                        // Found the main block — let onRemove handle the full teardown
                        pLevel.setBlock(neighbor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        break;
                    }
                }
            } else {
                // Main block removed — drop inventory, then remove all 8 additional blocks
                BlockEntity be = pLevel.getBlockEntity(pPos);
                if (be instanceof RocketControllerBlockEntity rcbe) {
                    rcbe.drops();
                }
                for (int[] off : OFFSETS) {
                    BlockPos neighbor = pPos.offset(off[0], 0, off[1]);
                    if (pLevel.getBlockState(neighbor).is(this)) {
                        pLevel.setBlock(neighbor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    // ── Ticker ────────────────────────────────────────────────────────

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || state.getValue(ADDITIONAL)) return null;
        return createTickerHelper(type, ROCKET_CONTROLLER_BE.get(),
                RocketControllerBlockEntity::tick);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    @Nullable
    private RocketControllerBlockEntity getMainBlockEntity(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(ADDITIONAL)) {
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof RocketControllerBlockEntity rcbe ? rcbe : null;
        }
        // Search all 8 neighbors for the main block
        for (int[] off : OFFSETS) {
            BlockPos neighbor = pos.offset(off[0], 0, off[1]);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.is(this) && !neighborState.getValue(ADDITIONAL)) {
                BlockEntity be = level.getBlockEntity(neighbor);
                return be instanceof RocketControllerBlockEntity rcbe ? rcbe : null;
            }
        }
        return null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
