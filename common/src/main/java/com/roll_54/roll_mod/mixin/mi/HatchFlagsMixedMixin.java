package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import com.roll_54.roll_mod.compat.mi.machines.MixedHatches;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * A mixed hatch ({@link MixedHatches}) fits wherever an item or a fluid hatch of its direction does, as Yet Another
 * Industrialization's do (its {@code HatchFlagsMixin}, Apache License 2.0).
 */
@Mixin(value = HatchFlags.class, remap = false)
public abstract class HatchFlagsMixedMixin {

    @Shadow
    @Final
    private Set<HatchType> allowed;

    @Inject(method = "allows", at = @At("HEAD"), cancellable = true)
    private void roll_mod$allowsMixed(HatchType type, CallbackInfoReturnable<Boolean> cir) {
        if (type == MixedHatches.INPUT
                && (allowed.contains(HatchTypes.ITEM_INPUT) || allowed.contains(HatchTypes.FLUID_INPUT))) {
            cir.setReturnValue(true);
        } else if (type == MixedHatches.OUTPUT
                && (allowed.contains(HatchTypes.ITEM_OUTPUT) || allowed.contains(HatchTypes.FLUID_OUTPUT))) {
            cir.setReturnValue(true);
        }
    }
}
