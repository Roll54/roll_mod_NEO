package com.roll_54.roll_mod.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Small ore outcrop that can sit on a floor or hang from a ceiling. Only the two vertical
 * orientations are valid: {@code vertical_direction=up} is the floor variant, {@code down} the
 * ceiling one (the model is flipped for it). Purely decorative geometry, so it has no collision.
 *
 * <p>Right-clicking one picks it at once: the sample is gone and the player gets one raw ore of the ore it stands
 * for ({@code raw_<ore>}). Mining it still drops raw ore as before.
 *
 * <p>Waterloggable, so samples in rivers, oceans and flooded caves keep their water. GTMOGS places vein indicators
 * with the default state; {@code SurfaceIndicatorWaterlogMixin} sets {@link #WATERLOGGED} when it replaces a water
 * source.
 */
public class OreSampleBlock extends Block implements SimpleWaterloggedBlock {

    public static final MapCodec<OreSampleBlock> CODEC = simpleCodec(OreSampleBlock::new);

    /** Vanilla's dripstone property: restricted to UP/DOWN, so no horizontal state can exist. */
    public static final DirectionProperty VERTICAL_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape SHAPE_UP = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 4.0D, 12.0D);
    private static final VoxelShape SHAPE_DOWN = Block.box(4.0D, 12.0D, 4.0D, 12.0D, 16.0D, 12.0D);

    public OreSampleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VERTICAL_DIRECTION, Direction.UP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VERTICAL_DIRECTION, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Clicking the underside of a block hangs the sample from it; anything else lays it on the floor.
        Direction direction = context.getClickedFace() == Direction.DOWN ? Direction.DOWN : Direction.UP;
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(VERTICAL_DIRECTION, direction).setValue(WATERLOGGED, water);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        ItemStack ore = oreOf(state);
        if (ore.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        // Its breaking particles and sound, then water back where it stood in water.
        level.levelEvent(2001, pos, Block.getId(state));
        level.setBlock(pos, state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        if (!player.getInventory().add(ore)) player.drop(ore, false);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        return InteractionResult.CONSUME;
    }

    /** Per sample block, the raw ore it gives ({@code raw_<ore>}, as its loot table drops when mined). */
    private static java.util.Map<Block, net.minecraft.world.item.Item> rawBySample;

    /** One raw ore of the ore this sample stands for, or empty when that ore has no raw item. */
    private static ItemStack oreOf(BlockState state) {
        net.minecraft.world.item.Item raw = raws().get(state.getBlock());
        return raw == null ? ItemStack.EMPTY : new ItemStack(raw);
    }

    private static synchronized java.util.Map<Block, net.minecraft.world.item.Item> raws() {
        if (rawBySample != null) return rawBySample;
        java.util.Map<Block, net.minecraft.world.item.Item> map = new java.util.HashMap<>();
        for (com.roll_54.roll_mod.data.datagen.ore.OreDefinition def
                : com.roll_54.roll_mod.data.datagen.ore.OreDefinitions.ORE_DEFINITIONS) {
            var raw = BuiltInRegistries.ITEM.getOptional(com.roll_54.roll_mod.RollMod.id("raw_" + def.id()));
            if (raw.isEmpty()) continue;
            for (var base : def.bases()) {
                BuiltInRegistries.BLOCK.getOptional(com.roll_54.roll_mod.RollMod.id(
                                com.roll_54.roll_mod.registry.GeneratedOreRegistry.oreSampleId(base, def.id())))
                        .ifPresent(sample -> map.put(sample, raw.get()));
            }
        }
        rawBySample = map;
        return map;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(VERTICAL_DIRECTION) == Direction.DOWN ? SHAPE_DOWN : SHAPE_UP;
    }
}
