package com.roll_54.roll_mod.economy.vendingblock.integration.carryon;

import com.roll_54.roll_mod.economy.vendingblock.block.DisplayBlock;
import com.roll_54.roll_mod.economy.vendingblock.block.VendorBlock;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.DisplayBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    BlockState state = level.getBlockState(pos);
    if (state.getBlock() instanceof DisplayBlock || state.getBlock() instanceof VendorBlock) {
      BlockEntity be = level.getBlockEntity(pos);
      if (be instanceof DisplayBlockEntity displayEntity) {
        if (!displayEntity.isOwner(player)) {
          info.setReturnValue(false);
        }
      }
      if (be instanceof VendorBlockEntity vendorEntity) {
        if (!vendorEntity.isOwner(player)) {
          info.setReturnValue(false);
        }
      }
    }
  }
}
