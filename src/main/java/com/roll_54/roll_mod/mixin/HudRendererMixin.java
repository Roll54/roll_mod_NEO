package com.roll_54.roll_mod.mixin;

import aztech.modern_industrialization.items.armor.HudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Mixin(HudRenderer.class)
public class HudRendererMixin {

    // замінюємо константу 16777216L = 1 << 24
    @ModifyConstant(
            method = "onRenderHud",
            constant = @Constant(longValue = 16777216L)
    )
    private static long replaceEnergyBarMax(long old) {
        return 1L << 30;
    }
}