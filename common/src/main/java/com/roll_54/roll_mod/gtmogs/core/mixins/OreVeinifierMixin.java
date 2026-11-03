package com.roll_54.roll_mod.gtmogs.core.mixins;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OreVeinifier.class)
public class OreVeinifierMixin {

    @Inject(
            method = "create(Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;)Lnet/minecraft/world/level/levelgen/NoiseChunk$BlockStateFiller;",
            at = @At("HEAD"),
            cancellable = true)
    private static void gtmogs$create(DensityFunction function1, DensityFunction function2, DensityFunction function3,
                                      PositionalRandomFactory random,
                                      CallbackInfoReturnable<NoiseChunk.BlockStateFiller> cir) {
        if (ConfigHolder.INSTANCE != null && ConfigHolder.INSTANCE.worldgen.oreVeins.removeVanillaLargeOreVeins)
            cir.setReturnValue(functionContext -> null);
    }
}
