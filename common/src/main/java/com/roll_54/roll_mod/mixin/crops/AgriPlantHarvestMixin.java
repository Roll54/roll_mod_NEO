package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.roll_54.roll_mod.compat.argicraft.CropBreeding;
import com.roll_54.roll_mod.compat.argicraft.CropBreedingSettings;
import com.roll_54.roll_mod.compat.argicraft.AgriCropNutrition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * N/P/K upkeep (crops wiki §11.7). {@code onHarvest} runs after every successful harvest — right-click,
 * {@code AgriApi.harvest}, Crop Managers, Create's harvester — so each collection pays one harvest's
 * N, P and K, or the crop's genes above {@value CropBreedingSettings#CEILING_UNSUPPLIED} fall back to it.
 */
@Mixin(AgriPlant.class)
public abstract class AgriPlantHarvestMixin {

    @Inject(method = "onHarvest", at = @At("HEAD"))
    private void roll_mod$nutrientUpkeep(AgriCrop crop, LivingEntity entity, CallbackInfo ci) {
        Level level = crop.getLevel();
        if (level == null || level.isClientSide || !(crop instanceof AgriCropNutrition nutrition)) {
            return;
        }
        if (nutrition.roll_mod$consumeNutrients()) {
            return;
        }
        if (crop.getGenome() != null
                && CropBreeding.regress(crop.getGenome(), CropBreedingSettings.CEILING_UNSUPPLIED)) {
            crop.asBlockEntity().setChanged();
            level.sendBlockUpdated(crop.getBlockPos(), crop.getBlockState(), crop.getBlockState(), 3);
        }
    }
}
