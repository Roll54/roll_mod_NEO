package com.roll_54.roll_mod.blocks;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.blocks.entity.HueriStatueBlockEntity;
import com.roll_54.roll_mod.data.RMMAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A statue rendered from a GeckoLib geo model - no animations. Right-click opens a page of text
 * about Hueri ({@link HueriStatueUI}).
 *
 * <p>Until a player has opened one — any statue, once, see {@link RMMAttachment#HUERI_STATUE_SEEN} —
 * every statue draws their eye: particles here, a floating "!" in its renderer and a look-at hint
 * ({@link HueriStatueClientEvents}).
 *
 * <p>Two blocks tall, the way a door is: the {@link #HALF lower} half holds the block entity and
 * draws the model, the upper half is an invisible block that exists so the statue's top can be
 * aimed at, clicked and broken — a ray only tests the cell it passes through, so a taller shape on
 * the lower half alone would never be hit up there. Either half's outline covers both, so the
 * statue highlights as one. Breaking either breaks both and drops one statue (the loot table only
 * drops for the lower half; the upper takes the lower with it). A statue placed before the upper
 * half existed grows one the next time it is clicked, if the space above is free.
 */
public class HueriStatueBlock extends BaseEntityBlock implements BlockUIMenuType.BlockUI {
    public static final MapCodec<HueriStatueBlock> CODEC = simpleCodec(HueriStatueBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape LOWER_OUTLINE = Block.box(0, 0, 0, 16, 32, 16);
    private static final VoxelShape UPPER_OUTLINE = Block.box(0, -16, 0, 16, 16, 16);

    public HueriStatueBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    /** GeoBlockRenderer picks up HORIZONTAL_FACING on its own and rotates the model to match. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** The other half's position, whether or not the other half is actually there. */
    private static BlockPos otherHalf(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
    }

    private static BlockPos lowerHalf(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    private boolean isOtherHalf(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(HALF) != state.getValue(HALF);
    }

    /**
     * Breaking the upper half in creative takes the lower one silently first, as a door does, so the
     * lower half's drop never happens; in survival the lower half goes in {@link #onRemove} and drops.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER
                && isOtherHalf(state, level.getBlockState(pos.below()))) {
            level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Whichever half goes, the other goes with it: the upper half takes the lower one with its drop
     * (the upper half itself drops nothing), the lower half takes the upper one without.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockPos other = otherHalf(state, pos);
            if (isOtherHalf(state, level.getBlockState(other))) {
                if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                    level.destroyBlock(other, true);
                } else {
                    level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Each half's outline spans both blocks, so aiming at either highlights the whole statue. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_OUTLINE : UPPER_OUTLINE;
    }

    /** Collision stays one cube per half: a shape reaching into the next cell is not reliably collided with. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return blockState.getValue(HALF) == DoubleBlockHalf.LOWER ? new HueriStatueBlockEntity(blockPos, blockState) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        BlockPos lower = lowerHalf(state, pos);
        if (level.isClientSide()) {
            // The open starts here, not on the server: only this client knows which players its
            // text names, and the server sends their names before the screen — see HueriStatuePlayers.
            HueriStatueText.requestOpen(lower);
        } else if (state.getValue(HALF) == DoubleBlockHalf.LOWER && level.getBlockState(pos.above()).isAir()
                && pos.getY() < level.getMaxBuildHeight() - 1) {
            // A statue from before the upper half existed: give it one, if the space is free.
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return HueriStatueUI.createUI(holder.player, holder.pos);
    }

    /**
     * Client only (the level calls it for blocks near the camera): a slow ring of sparkles round the
     * statue's 2-block height while this player has not read it yet.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (ClientHueriStatue.seen || state.getValue(HALF) == DoubleBlockHalf.UPPER) return;
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        float turn = (level.getGameTime() % 80L) / 80.0F * Mth.TWO_PI;
        for (int i = 0; i < 2; i++) {
            float angle = turn + i * Mth.PI + random.nextFloat() * 0.4F;
            double y = pos.getY() + 0.1 + random.nextDouble() * 1.9;
            level.addParticle(ParticleTypes.END_ROD, cx + Mth.cos(angle) * 0.75, y, cz + Mth.sin(angle) * 0.75,
                    0.0, 0.01, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, cx + (random.nextDouble() - 0.5) * 1.6,
                    pos.getY() + 1.0 + random.nextDouble(), cz + (random.nextDouble() - 0.5) * 1.6,
                    (random.nextDouble() - 0.5) * 0.5, 0.3, (random.nextDouble() - 0.5) * 0.5);
        }
    }

    /**
     * The geo model is drawn by the lower half's block entity renderer, not by the chunk mesh; the
     * upper half draws nothing at all.
     */
    @Override
    protected RenderShape getRenderShape(BlockState blockState) {
        return blockState.getValue(HALF) == DoubleBlockHalf.LOWER ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }
}
