package com.roll_54.roll_mod.mixin.crops;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.agricraft.agricraft.compat.jei.CropRequirementCategory$Recipe")
public abstract class CropRequirementCategoryRecipeMixin {

    @Shadow
    private List<Block> soils;

    @Shadow
    private int soil;

    @Inject(method = "updateSoils", at = @At("TAIL"))
    private void roll_mod$ensureSoilsFallback(CallbackInfo ci) {
        if (this.soils == null || this.soils.isEmpty()) {
            this.soils = List.of(Blocks.BEDROCK);
            this.soil = 0;
        }
    }
}

