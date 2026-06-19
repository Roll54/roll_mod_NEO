package com.roll_54.roll_mod_server.mixin.prime;

import com.roll_54.roll_mod_server.minestar.PrimeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class MixinServerPlayerEntityPrimeFeature {
     /*
    @author ProstoMaslo
    @reasone Save inventory
    */

    @Inject(
            method = "restoreFrom",
            at = @At("HEAD"),
            cancellable = true
    )

    private void onCopyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer)(Object)this;

        if (PrimeManager.hasPrime(self)) {
            self.getInventory().replaceWith(oldPlayer.getInventory());
            self.setHealth(oldPlayer.getHealth());
            self.experienceLevel    = oldPlayer.experienceLevel;
            self.totalExperience    = oldPlayer.totalExperience;
            self.experienceProgress = oldPlayer.experienceProgress;

            for (MobEffectInstance e : oldPlayer.getActiveEffects()) {
                self.addEffect(e);
            }
        }
    }
}