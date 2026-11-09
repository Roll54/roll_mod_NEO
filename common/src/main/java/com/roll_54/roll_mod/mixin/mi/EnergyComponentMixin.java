package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.machines.components.EnergyComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * {@code consumeEu}/{@code insertEu} always set the stored EU and marked the machine changed (chunk dirty, comparator
 * update), also when nothing moved: an idle machine asking for energy, a full buffer offered more. When the stored
 * amount would not change there is nothing to save or report, so the call is skipped.
 */
@Mixin(value = EnergyComponent.class, remap = false)
public abstract class EnergyComponentMixin {
    @Shadow
    private long storedEu;

    @Shadow
    @Final
    private Supplier<Long> capacity;

    @Inject(method = "setEu", at = @At("HEAD"), cancellable = true)
    private void roll_mod$skipUnchanged(long eu, boolean update, CallbackInfo ci) {
        if (Math.min(eu, this.capacity.get()) == this.storedEu) {
            ci.cancel();
        }
    }
}
