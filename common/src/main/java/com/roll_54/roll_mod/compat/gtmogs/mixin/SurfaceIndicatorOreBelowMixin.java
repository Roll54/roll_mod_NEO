package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.roll_54.roll_mod.compat.gtmogs.OreSampleVeins;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A surface sample is only placed where its ore is straight beneath it, so a sample always marks real ore.
 * GTMOGS scatters a vein's samples around the vein's centre whether or not the vein reached that column.
 * Samples go in after the chunk's ores ({@code OrePlacer.placeOres}, then {@code placeIndicators}), so the
 * column below is already final. See {@link OreSampleVeins}.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.api.worldgen.generator.indicators.SurfaceIndicatorGenerator")
public abstract class SurfaceIndicatorOreBelowMixin {

    @Inject(method = "placeIndicator", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$onlyOverOre(BulkSectionAccess access, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!OreSampleVeins.hasOreBelow(access, pos, state)) ci.cancel();
    }
}
