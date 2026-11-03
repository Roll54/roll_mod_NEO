package com.roll_54.roll_mod.mixin;

import com.roll_54.roll_mod.worldgen.MiningWorld;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The mining world has no weather. Its weather is the overworld's (it shares the overworld's level
 * data), so when it rains there, rain and thunder levels read as zero here: no rain or darker sky on the
 * client, no lightning or rain-filled cauldrons on the server.
 */
@Mixin(Level.class)
public abstract class MiningWorldWeatherMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void roll_mod$noRain(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (((Level) (Object) this).dimension() == MiningWorld.LEVEL) cir.setReturnValue(0.0F);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void roll_mod$noThunder(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (((Level) (Object) this).dimension() == MiningWorld.LEVEL) cir.setReturnValue(0.0F);
    }
}
