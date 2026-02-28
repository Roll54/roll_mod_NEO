package com.roll_54.roll_mod.blocks;

import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.blocks.entity.ResearchWorkbenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.roll_54.roll_mod.init.BlockEntites.RESEARCH_WORKBENCH_BE;

public class ResearchWorkbenchBlock extends BaseEntityBlock {

    public static final MapCodec<ResearchWorkbenchBlock> CODEC = simpleCodec(ResearchWorkbenchBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;

    public ResearchWorkbenchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TYPE, ChestType.LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos rightPos = context.getClickedPos().relative(facing.getClockWise());
        // Do not place if the right part is blocked
        if (!context.getLevel().getBlockState(rightPos).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(TYPE, ChestType.LEFT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Direction facing = state.getValue(FACING);
        BlockPos rightPos = pos.relative(facing.getClockWise());
        BlockState rightState = state.setValue(TYPE, ChestType.RIGHT);
        level.setBlock(rightPos, rightState, Block.UPDATE_ALL);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchWorkbenchBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            ChestType type = pState.getValue(TYPE);
            Direction facing = pState.getValue(FACING);
            BlockPos otherPos = pPos.relative(type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise());

            if (type == ChestType.LEFT) {
                BlockEntity be = pLevel.getBlockEntity(pPos);
                if (be instanceof ResearchWorkbenchBlockEntity rwbe) {
                    rwbe.drops();
                }
            }

            BlockState otherState = pLevel.getBlockState(otherPos);
            if (otherState.is(this)) {
                pLevel.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos,
                                              Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
        if (!pLevel.isClientSide()) {
            ResearchWorkbenchBlockEntity mainBe = getMainBlockEntity(pLevel, pPos, pState);
            if (mainBe != null) {
                ((ServerPlayer) pPlayer).openMenu(
                        new SimpleMenuProvider(mainBe, Component.translatable("block.roll_mod.research_workbench")),
                        mainBe.getBlockPos());
            } else {
                throw new IllegalStateException("ResearchWorkbench block entity missing!");
            }
        }
        return ItemInteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Nullable
    private ResearchWorkbenchBlockEntity getMainBlockEntity(Level level, BlockPos pos, BlockState state) {
        BlockPos mainPos = pos;
        if (state.getValue(TYPE) == ChestType.RIGHT) {
            mainPos = pos.relative(state.getValue(FACING).getCounterClockWise());
        }
        BlockEntity be = level.getBlockEntity(mainPos);
        return be instanceof ResearchWorkbenchBlockEntity rwbe ? rwbe : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || state.getValue(TYPE) == ChestType.RIGHT) return null;
        return createTickerHelper(type, RESEARCH_WORKBENCH_BE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
