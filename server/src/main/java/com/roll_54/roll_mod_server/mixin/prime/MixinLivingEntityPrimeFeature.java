package com.roll_54.roll_mod_server.mixin.prime;


import com.roll_54.roll_mod_server.minestar.PrimeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntityPrimeFeature {
    /*
    @author ProstoMaslo
    @reasone Cancel drop inventory
    */

    @Inject(
            method = "dropAllDeathLoot",
            at = @At("HEAD"),
            cancellable = true
    )

    private void keepInventoryForPrime(ServerLevel p_level, DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof ServerPlayer player) {
            if (PrimeManager.hasPrime(player)) {
                ci.cancel();
            }
        }
    }
}