package com.roll_54.roll_mod.compat.sophisticated.mixin;

import com.roll_54.roll_mod.compat.sophisticated.TankTierUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlock;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sophisticated Storage's own tier upgrades leave tanks alone: tanks take roll_mod's tank upgrades
 * ({@link TankTierUpgradeItem}) only. Chests, barrels and shulker boxes upgrade as before.
 */
@Mixin(value = StorageTierUpgradeItem.class, remap = false)
public abstract class StorageTierUpgradeItemMixin {

    @Inject(method = "tryUpgradeStorage(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void roll_mod$tanksTakeTankUpgradesOnly(ItemStack stack, Level level, BlockPos pos, BlockState state,
                                                    Player player, CallbackInfoReturnable<InteractionResult> cir) {
        if (state.getBlock() instanceof TankBlock && !((Object) this instanceof TankTierUpgradeItem)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
