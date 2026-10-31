package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.genetic.AgriMutationHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.roll_54.roll_mod.compat.argicraft.CropBreedingSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Mutativity keeps its 1–10 meaning with stats stored up to 20: 10 and above always drift up. */
@Mixin(AgriMutationHandler.DefaultStatMutator.class)
public abstract class DefaultStatMutatorMixin {

    @ModifyExpressionValue(
            method = "rollAndExecuteMutation",
            at = @At(value = "INVOKE", target = "Lcom/agricraft/agricraft/api/stat/AgriStat;getMax()I")
    )
    private int roll_mod$mutativityReferenceMax(int max) {
        return CropBreedingSettings.STAT_REFERENCE_MAX;
    }
}
