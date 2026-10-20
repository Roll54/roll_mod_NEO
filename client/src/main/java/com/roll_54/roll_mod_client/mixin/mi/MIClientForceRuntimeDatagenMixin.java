package com.roll_54.roll_mod_client.mixin.mi;

import aztech.modern_industrialization.client.MIClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Always runs MI's client runtime datagen, whatever {@code datagenOnStartup} says in MI's startup
 * config, so MI regenerates its part textures from the templates {@link MITextureManagerOverrideMixin}
 * reroutes to ours. Read in MI 2.4.1: {@code MIClient.init} (FMLConstructModEvent) only registers the
 * AddPackFindersEvent listener that runs {@code RuntimeDataGen} when that option is true.
 */
@Mixin(MIClient.class)
public class MIClientForceRuntimeDatagenMixin {
    @ModifyExpressionValue(method = "init",
            slice = @Slice(from = @At(value = "FIELD", target = "Laztech/modern_industrialization/config/MIStartupConfig;datagenOnStartup:Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;")),
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;getAsBoolean()Z"))
    private static boolean roll_mod$alwaysDatagen(boolean configured) {
        return true;
    }
}
