package com.roll_54.roll_mod.mixin;

import aztech.modern_industrialization.items.armor.GraviChestPlateItem;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GraviChestPlateItem.class)
public class GraviChestPlateItemMixin {

    @ModifyConstant(method = "getEnergyCapacity", constant = @Constant(longValue = 1 << 24))
    private long injectedCapacity(long old) {
        return 1L << 30;
    }

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void injectedBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        long energy = ((ISimpleEnergyItem)this).getStoredEnergy(stack);
        long cap = 1L << 30;

        int width = (int) Math.round(energy / (double) cap * 13);
        cir.setReturnValue(width);
    }
}