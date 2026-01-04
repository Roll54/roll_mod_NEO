package com.roll_54.roll_mod.mixin;

import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {

    @ModifyConstant(
            method = "tick",
            constant = @Constant(intValue = 72000)
    )
    private int roll_mod$increasePhantomDelay(int original) {
        return original * 8;
    }
}
