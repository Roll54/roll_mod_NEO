package com.roll_54.roll_mod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * A placed whole dish that is eaten or sliced a quarter at a time, the way Farmer's Delight's
 * {@code PieBlock} works: right-click empty-handed eats one slice in place, right-click with a knife
 * pops one slice item off. Written standalone because Farmer's Delight is only an optional dependency,
 * so nothing here may name its classes; knives are matched by tag instead.
 */
@Deprecated // No dish uses it any more: the pie is Farmer's Delight's PieBlock, the pizza Brewin' and Chewin''s.
public class SliceableFoodBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 0..3 = quarters eaten; {@link #EMPTY_PLATE} = all eaten, only the plate is left (plated dishes only). */
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 4);
    public static final int MAX_BITES = 4;
    public static final int EMPTY_PLATE = MAX_BITES;

    private static final TagKey<Item> KNIVES_C =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/knife"));
    private static final TagKey<Item> KNIVES_FD =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("farmersdelight", "tools/knives"));
    private static final ResourceLocation FD_SLICE_SOUND =
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "block.food.slice");

    private final Supplier<? extends Item> slice;
    /** [bites][Direction#get2DDataValue] */
    private final VoxelShape[][] shapes;
    private final boolean plated;

    /**
     * @param height      thickness in pixels; 4 matches a Farmer's Delight pie, a pizza is flatter.
     * @param margin      gap in pixels between the food and the block edge (2 = a 12x12 dish, 1 = 14x14).
     * @param plateHeight thickness of the plate the food rests on (part of every stage's model, and the
     *                    {@code _empty} model alone once eaten), 0 for none; the food sits this much higher.
     * @param plateMargin gap in pixels between the plate and the block edge.
     */
    public SliceableFoodBlock(Properties properties, Supplier<? extends Item> slice,
                              int height, int margin, int plateHeight, int plateMargin) {
        super(properties);
        this.slice = slice;
        this.shapes = buildShapes(height, margin, plateHeight, plateMargin);
        this.plated = plateHeight > 0;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BITES, 0));
    }

    public ItemStack getSliceItem() {
        return new ItemStack(slice.get());
    }

    // Shapes mirror the block models for a north-facing block: whole, then three/two/one quarters left.
    private static VoxelShape[][] buildShapes(int h, int m, int plate, int pm) {
        int top = plate + h;
        int far = 16 - m;
        VoxelShape[] north = {
                Block.box(m, plate, m, far, top, far),
                Shapes.or(Block.box(m, plate, 8, 8, top, far), Block.box(m, plate, m, far, top, 8)),
                Block.box(m, plate, m, far, top, 8),
                Block.box(8, plate, m, far, top, 8)
        };
        // Square and centred, so it needs no rotation.
        VoxelShape plateShape = plate > 0 ? Block.box(pm, 0, pm, 16 - pm, plate, 16 - pm) : Shapes.empty();
        VoxelShape[][] out = new VoxelShape[north.length + 1][4];
        for (int bites = 0; bites < north.length; bites++) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                out[bites][dir.get2DDataValue()] = Shapes.or(plateShape, rotateFromNorth(north[bites], dir));
            }
        }
        // Unreachable for unplated dishes (they vanish on the last bite), but keep a valid shape anyway.
        Arrays.fill(out[EMPTY_PLATE], plate > 0 ? plateShape : Block.box(m, 0, m, far, 1, far));
        return out;
    }

    // Same clockwise rotation the blockstate's "y" applies (east = 90, south = 180, west = 270).
    private static VoxelShape rotateFromNorth(VoxelShape shape, Direction to) {
        int turns = switch (to) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        VoxelShape result = shape;
        for (int i = 0; i < turns; i++) {
            VoxelShape rotated = Shapes.empty();
            for (AABB box : result.toAabbs()) {
                rotated = Shapes.or(rotated, Shapes.box(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX));
            }
            result = rotated;
        }
        return result;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(BITES)][state.getValue(FACING).get2DDataValue()];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(BITES) == EMPTY_PLATE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(KNIVES_C) || stack.is(KNIVES_FD)) {
            return cutSlice(level, pos, state, player, stack.getItem());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(BITES) == EMPTY_PLATE) {
            return takePlate(level, pos);
        }
        if (level.isClientSide) {
            if (consumeBite(level, pos, state, player).consumesAction()) return InteractionResult.SUCCESS;
            if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return InteractionResult.CONSUME;
        }
        return consumeBite(level, pos, state, player);
    }

    protected InteractionResult consumeBite(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.canEat(false)) return InteractionResult.PASS;

        ItemStack sliceStack = getSliceItem();
        FoodProperties food = sliceStack.getItem().getFoodProperties(sliceStack, player);
        if (food != null) {
            player.getFoodData().eat(food);
            if (!level.isClientSide) {
                for (FoodProperties.PossibleEffect effect : food.effects()) {
                    if (level.random.nextFloat() < effect.probability()) {
                        player.addEffect(effect.effect());
                    }
                }
            }
        }

        removeBite(level, pos, state);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 0.8F);
        return InteractionResult.SUCCESS;
    }

    protected ItemInteractionResult cutSlice(Level level, BlockPos pos, BlockState state, Player player, Item knife) {
        removeBite(level, pos, state);

        Direction toward = player.getDirection().getOpposite();
        ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, getSliceItem(),
                toward.getStepX() * 0.15, 0.05, toward.getStepZ() * 0.15);
        level.addFreshEntity(drop);

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(FD_SLICE_SOUND).orElse(SoundEvents.WOOL_BREAK);
        level.playSound(null, pos, sound, SoundSource.PLAYERS, 0.8F, 0.8F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.001);
        }
        player.awardStat(Stats.ITEM_USED.get(knife));
        return ItemInteractionResult.SUCCESS;
    }

    private void removeBite(Level level, BlockPos pos, BlockState state) {
        int bites = state.getValue(BITES);
        if (bites < MAX_BITES - 1) {
            level.setBlock(pos, state.setValue(BITES, bites + 1), Block.UPDATE_ALL);
        } else if (plated) {
            level.setBlock(pos, state.setValue(BITES, EMPTY_PLATE), Block.UPDATE_ALL);
        } else {
            level.removeBlock(pos, false);
        }
    }

    /** Right-clicking the leftover plate picks it up as a bowl. */
    protected InteractionResult takePlate(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            level.removeBlock(pos, false);
            Block.popResource(level, pos, new ItemStack(Items.BOWL));
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
        builder.add(FACING, BITES);
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return MAX_BITES - state.getValue(BITES);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
