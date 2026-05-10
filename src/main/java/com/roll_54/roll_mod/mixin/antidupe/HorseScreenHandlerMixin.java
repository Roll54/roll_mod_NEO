package com.roll_54.roll_mod.mixin.antidupe;

import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HorseInventoryMenu.class)
public class HorseScreenHandlerMixin {

    @Shadow @Final private AbstractHorse horse;

    @Inject(
            method = "quickMoveStack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onlyTransferIfEntityAlive(Player player, int i, CallbackInfoReturnable<ItemStack> cir) {
        if (player.isDeadOrDying() || player.isRemoved() || this.horse.isDeadOrDying() || this.horse.isRemoved()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}