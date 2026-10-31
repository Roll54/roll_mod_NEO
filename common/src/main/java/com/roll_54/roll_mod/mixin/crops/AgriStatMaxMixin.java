package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.stat.AgriStat;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.roll_54.roll_mod.compat.argicraft.CropBreedingSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stats run 1–{@value CropBreedingSettings#STAT_MAX}, ignoring {@code <stat>_max} in AgriCraft's
 * config (crops wiki §1). The formulas that scale by the max keep
 * {@value CropBreedingSettings#STAT_REFERENCE_MAX} as the top instead: fertility
 * ({@code AgriCrossBreedEngineMixin}), resistance ({@code CropBlockEntityMixin}), mutativity
 * ({@code DefaultStatMutatorMixin}) and the season bypass ({@code AgriSeasonConditionMixin}).
 *
 * <p>{@code GeneStat} builds its allele cache from {@code getMin()..getMax()} when it is constructed,
 * so this has to hold from the first call — a value above the cached range comes back as a null allele.
 */
@Mixin(AgriStat.class)
public abstract class AgriStatMaxMixin {

    @ModifyReturnValue(method = "getMax", at = @At("RETURN"))
    private int roll_mod$statMax(int max) {
        return CropBreedingSettings.STAT_MAX;
    }
}
