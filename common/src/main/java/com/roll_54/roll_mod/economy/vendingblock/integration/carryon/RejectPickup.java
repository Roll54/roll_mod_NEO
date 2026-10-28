package com.roll_54.roll_mod.economy.vendingblock.integration.carryon;

import com.roll_54.roll_mod.economy.service.ServiceBlock;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.OwnedBlockEntity;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tschipp.carryon.common.carry.PickupHandler;

@Mixin(PickupHandler.class)
public class RejectPickup {

  @Inject(method = "tryPickUpBlock", at = @At("HEAD"), cancellable = true)
  private static void tryPickUpBlock(
      ServerPlayer player,
      BlockPos pos,
      Level level,
      BiFunction<BlockState, BlockPos, Boolean> pickupCallback,
      CallbackInfoReturnable<Boolean> info) {
    if (level.getBlockEntity(pos) instanceof OwnedBlockEntity owned && !owned.isOwner(player)) {
      info.setReturnValue(false);
    }
    // The service block is carried by its lower half, which holds the block entity; the upper half
    // alone would be picked up as an empty block.
    BlockState state = level.getBlockState(pos);
    if (state.getBlock() instanceof ServiceBlock && state.getValue(ServiceBlock.HALF) == DoubleBlockHalf.UPPER) {
      info.setReturnValue(false);
    }
  }
}
