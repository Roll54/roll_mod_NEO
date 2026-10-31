package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.requirement.AgriGrowthConditionRegistry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.roll_54.roll_mod.compat.argicraft.CropBreedingSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Strength 10 and above still ignores seasons, with stats stored up to 20. {@code lambda$new$16} is
 * the season condition's check in AgriCraft 4.0.24 — the only {@code getMax()} call in this class.
 */
@Mixin(AgriGrowthConditionRegistry.class)
public abstract class AgriSeasonConditionMixin {

    @ModifyExpressionValue(
            method = "lambda$new$16",
            at = @At(value = "INVOKE", target = "Lcom/agricraft/agricraft/api/stat/AgriStat;getMax()I")
    )
    private static int roll_mod$seasonReferenceMax(int max) {
        return CropBreedingSettings.STAT_REFERENCE_MAX;
    }
}
