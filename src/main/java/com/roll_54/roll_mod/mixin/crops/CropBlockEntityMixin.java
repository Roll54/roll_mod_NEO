package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.plant.AgriWeed;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlockEntity.class)
public abstract class CropBlockEntityMixin {

    @Shadow
    private String weedId;

    @Shadow
    private AgriWeed weed;

    @Shadow
    private AgriGrowthStage weedGrowthStage;

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void roll_mod$clearWeedsWhenTagSaysNo(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {
        if (!tag.getBoolean("hasWeeds")) {
            this.weedId = "";
            this.weed = null;
            this.weedGrowthStage = new AgriGrowthStage(0, 8);
        }
    }


}