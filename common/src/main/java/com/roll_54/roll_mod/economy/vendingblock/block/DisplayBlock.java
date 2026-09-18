package com.roll_54.roll_mod.economy.vendingblock.block;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.DisplayBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.display.DisplayUI;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;

public class DisplayBlock extends BaseEntityBlock implements BlockUIMenuType.BlockUI {

  public static final VoxelShape SHAPE_DOME = Block.box(1, 0, 1, 15, 15, 15);
  public static final VoxelShape SHAPE_BASE = Block.box(0, 0, 0, 16, 2, 16);
  public static final VoxelShape SHAPE = Shapes.or(SHAPE_BASE, SHAPE_DOME);
  public static final MapCodec<DisplayBlock> CODEC = simpleCodec(DisplayBlock::new);

  public DisplayBlock(Properties properties) {
    super(properties);
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPE;
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
  public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
    return new DisplayBlockEntity(blockPos, blockState);
  }

  @Override
  public void setPlacedBy(
      Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);

    if (placer instanceof Player player
        && level.getBlockEntity(pos) instanceof DisplayBlockEntity displayBlockEntity) {
      displayBlockEntity.setOwner(player);
    }
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
    if (level.getBlockEntity(pos) instanceof DisplayBlockEntity displayBlockEntity
        && !level.isClientSide()) {
      if (displayBlockEntity.isOwner(player)
          && ModList.get().isLoaded("carryon")
          && player.isShiftKeyDown()) {
        return InteractionResult.SUCCESS; // Allows owners to pick up the display block with CarryOn
      }

      if (displayBlockEntity.isOwner(player)) {
        displayBlockEntity.updateOwnershipInfo(player);
        BlockUIMenuType.openUI((ServerPlayer) player, pos);
        level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 2.0F);
      }
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
    Player player = holder.player;
    if (!(player.level().getBlockEntity(holder.pos)
        instanceof DisplayBlockEntity displayBlockEntity)) {
      return ModularUI.of(UI.empty(), player);
    }
    return DisplayUI.build(displayBlockEntity, player);
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
    if (state.getBlock() != newState.getBlock()) {
      if (level.getBlockEntity(pos) instanceof DisplayBlockEntity displayBlockEntity) {
        displayBlockEntity.drops();
        level.updateNeighbourForOutputSignal(pos, this);
      }
    }
    super.onRemove(state, level, pos, newState, movedByPiston);
  }
}
