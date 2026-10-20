package com.roll_54.roll_mod.blocks;

import com.roll_54.roll_mod.items.XpFoodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * A whole dish served on a plate: placed by sneak-right-clicking its food item onto a block
 * ({@link XpFoodItem}), eaten in one go by right-clicking it, after which only the plate is left;
 * right-clicking the plate picks it up as a bowl, the same as {@link SliceableFoodBlock}'s plate.
 */
public class PlatedFoodBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty EATEN = BooleanProperty.create("eaten");

    private static final VoxelShape PLATE = Block.box(1, 0, 1, 15, 1, 15);

    private final Supplier<? extends Item> food;
    private final VoxelShape fullShape;

    /** @param foodHeight how far above the block's floor the food reaches, plate included, in pixels. */
    public PlatedFoodBlock(Properties properties, Supplier<? extends Item> food, int foodHeight) {
        super(properties);
        this.food = food;
        this.fullShape = Shapes.or(PLATE, Block.box(2, 1, 2, 14, foodHeight, 14));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(EATEN, false));
    }

    public ItemStack getFoodItem() {
        return new ItemStack(food.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(EATEN) ? PLATE : fullShape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(EATEN)) {
            if (!level.isClientSide) {
                level.removeBlock(pos, false);
                Block.popResource(level, pos, new ItemStack(Items.BOWL));
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        ItemStack stack = getFoodItem();
        FoodProperties props = stack.getFoodProperties(player);
        if (props == null || !player.canEat(props.canAlwaysEat())) return InteractionResult.PASS;
        if (!level.isClientSide) {
            player.getFoodData().eat(props);
            for (FoodProperties.PossibleEffect effect : props.effects()) {
                if (level.random.nextFloat() < effect.probability()) player.addEffect(effect.effect());
            }
            if (stack.getItem() instanceof XpFoodItem xpFood && xpFood.experiencePoints() > 0) {
                player.giveExperiencePoints(xpFood.experiencePoints());
            }
            level.setBlock(pos, state.setValue(EATEN, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return state.getValue(EATEN) ? new ItemStack(Items.BOWL) : getFoodItem();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                     LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        return facing == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportRigidBlock(level, pos.below());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, EATEN);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
