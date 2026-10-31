package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.genetic.AgriCrossBreedEngine;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.roll_54.roll_mod.compat.argicraft.CropBreeding;
import com.roll_54.roll_mod.compat.argicraft.CropBreedingSettings;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gene ceilings and the mutagen gate on cross-breeding (crops wiki §11.7).
 *
 * <p>{@code doCombine} is the only place with both parent crops in scope, so the breed's
 * {@link CropBreeding.Context} is opened around its {@code combine} call: the species mutator
 * ({@code DefaultPlantMutatorMixin}) reads the gate from it and records a mutation in it, and the
 * finished child is then either reset to default genes (mutation) or capped at the ceiling.
 */
@Mixin(AgriCrossBreedEngine.class)
public abstract class AgriCrossBreedEngineMixin {

    @WrapOperation(
            method = "doCombine",
            at = @At(value = "INVOKE",
                    target = "Lcom/agricraft/agricraft/api/genetic/AgriCrossBreedEngine$CombineLogic;combine(Lcom/agricraft/agricraft/api/crop/AgriCrop;Lcom/agricraft/agricraft/api/genetic/AgriGenome;Lcom/agricraft/agricraft/api/genetic/AgriGenome;Lnet/minecraft/util/RandomSource;)Lcom/agricraft/agricraft/api/genetic/AgriGenome;")
    )
    private AgriGenome roll_mod$gateAndCapChild(
            AgriCrossBreedEngine.CombineLogic combiner, AgriCrop target, AgriGenome genomeA, AgriGenome genomeB,
            RandomSource random, Operation<AgriGenome> original,
            @Local(argsOnly = true, ordinal = 1) AgriCrop parentA,
            @Local(argsOnly = true, ordinal = 2) AgriCrop parentB
    ) {
        CropBreeding.Context context = CropBreeding.open(parentA, parentB);
        try {
            AgriGenome child = original.call(combiner, target, genomeA, genomeB, random);
            if (context.mutatedTo != null) {
                // A new species starts from scratch: both species alleles the child, every stat at its minimum.
                Level level = target.getLevel();
                AgriPlant plant = (level != null
                        ? AgriApi.getPlant(context.mutatedTo, level.registryAccess())
                        : AgriApi.getPlant(context.mutatedTo)).orElse(null);
                if (plant != null) {
                    return new AgriGenome(plant);
                }
            }
            CropBreeding.applyCeiling(child, genomeA, genomeB, context.ceiling);
            return child;
        } finally {
            CropBreeding.close();
        }
    }

    /** Fertility keeps its 1–10 meaning with stats stored up to 20: fertility 10+ is always picked. */
    @ModifyExpressionValue(
            method = "rollFertility",
            at = @At(value = "INVOKE", target = "Lcom/agricraft/agricraft/api/stat/AgriStat;getMax()I")
    )
    private int roll_mod$fertilityReferenceMax(int max) {
        return CropBreedingSettings.STAT_REFERENCE_MAX;
    }
}
