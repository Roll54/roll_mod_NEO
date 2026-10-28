package com.roll_54.roll_mod.economy.service;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.economy.service.gui.ServiceUI;
import com.roll_54.roll_mod.economy.vendingblock.registry.BlockEntityRegistry;
import com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;

/**
 * A block a player places to sell a service — charging, XP, fluid — rather than items. The owner picks
 * the service on first open ({@link ServiceUI}); after that the block is fixed to it.
 *
 * <p>Ownership, break protection and the screen routing (vendor key / owner / customer) work as on the
 * vending block.
 *
 * <p>Two blocks tall, the way a door is, and turned to face the player who placed it: the {@link #HALF
 * lower} half holds the block entity, the upper half is there so the top can be aimed at, clicked and
 * broken, and hands every click to the lower half. Breaking either breaks both and drops one block (the
 * loot table only drops for the lower half). A block placed before the upper half existed grows one
 * the next time it is clicked, if the space above is free.
 */
public class ServiceBlock extends BaseEntityBlock implements BlockUIMenuType.BlockUI {

  public static final MapCodec<ServiceBlock> CODEC = simpleCodec(ServiceBlock::new);
  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
  public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

  private static final VoxelShape LOWER_OUTLINE = Block.box(0, 0, 0, 16, 32, 16);
  private static final VoxelShape UPPER_OUTLINE = Block.box(0, -16, 0, 16, 16, 16);

  public ServiceBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
        .setValue(HALF, DoubleBlockHalf.LOWER));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, HALF);
  }

  /** Where the block entity lives, from either half. */
  public static BlockPos lowerHalf(BlockState state, BlockPos pos) {
    return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
  }

  /** The other half's position, whether or not the other half is actually there. */
  private static BlockPos otherHalf(BlockState state, BlockPos pos) {
    return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
  }

  private boolean isOtherHalf(BlockState state, BlockState other) {
    return other.is(this) && other.getValue(HALF) != state.getValue(HALF);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    BlockPos pos = context.getClickedPos();
    Level level = context.getLevel();
    if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
      return null;
    }
    return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
  }

  /** The upper half for a lower half placed any other way than by hand (CarryOn), if there is room. */
  @Override
  protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
    super.onPlace(state, level, pos, oldState, movedByPiston);
    if (!level.isClientSide()) growUpperHalf(state, level, pos);
  }

  private static void growUpperHalf(BlockState state, Level level, BlockPos pos) {
    if (state.getValue(HALF) == DoubleBlockHalf.LOWER && level.getBlockState(pos.above()).isAir()
        && pos.getY() < level.getMaxBuildHeight() - 1) {
      level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }
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

  /** Each half's outline spans both blocks, so aiming at either highlights the whole machine. */
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
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ServiceBlockEntity(pos, state) : null;
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide() || state.getValue(HALF) == DoubleBlockHalf.UPPER
        ? null
        : createTickerHelper(type, BlockEntityRegistry.SERVICE_BE.get(), ServiceBlockEntity::serverTick);
  }

  @Override
  public void setPlacedBy(
      Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    if (placer instanceof Player player && level.getBlockEntity(pos) instanceof ServiceBlockEntity be) {
      be.setOwner(player);
      // The seller picks the service right away. Deferred until the placement has finished, and the
      // block's state sent first so the client builds the same picker the server does.
      if (player instanceof ServerPlayer sp && be.getServiceType() == null) {
        sp.server.execute(() -> {
          if (be.isRemoved() || sp.hasDisconnected()) return;
          be.sendTo(sp);
          BlockUIMenuType.openUI(sp, pos);
        });
      }
    }
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos clicked, Player player, BlockHitResult hitResult) {
    BlockPos pos = lowerHalf(state, clicked);
    if (!level.isClientSide()) growUpperHalf(state, level, clicked);
    if (level.getBlockEntity(pos) instanceof ServiceBlockEntity be) {
      if (be.isOwner(player) && ModList.get().isLoaded("carryon") && player.isShiftKeyDown()) {
        return InteractionResult.SUCCESS; // lets the owner pick the block up with CarryOn
      }
      if (!level.isClientSide()) {
        // The client builds the screen from its copy of the block; make sure that copy is current.
        be.sendTo((ServerPlayer) player);
        BlockUIMenuType.openUI((ServerPlayer) player, pos);
        level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 2.0F);
      }
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
    if (state.getBlock() != newState.getBlock()) {
      if (level.getBlockEntity(pos) instanceof ServiceBlockEntity be) {
        be.dropContents();
      }
      // Whichever half goes, the other goes with it: the upper half takes the lower one with its drop
      // (the upper half itself drops nothing), the lower half takes the upper one without.
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

  @Override
  public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
    Player player = holder.player;
    if (!(player.level().getBlockEntity(holder.pos) instanceof ServiceBlockEntity be)) {
      return ModularUI.of(UI.empty(), player);
    }
    boolean key = player.getMainHandItem().is(ItemRegistry.VENDOR_KEY.get());
    if (be.getServiceType() == null) {
      // Only the owner (or the key) picks; anyone else is told the block is not ready yet.
      return key || be.isOwner(player) ? ServiceUI.picker(be, player) : ServiceUI.notReady(be, player);
    }
    if (key || be.isOwner(player)) return ServiceUI.owner(be, player, key);
    return ServiceUI.customer(be, player);
  }
}
