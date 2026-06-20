package com.roll_54.roll_mod_server.mixin.prime;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.roll_54.roll_mod_server.minestar.PvpManager.TAG_PVP_DISABLED;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker instanceof FakePlayer) return;
        LivingEntity thisLivingEntity = (LivingEntity) (Object) this;
        if (!(thisLivingEntity instanceof ServerPlayer victim) || victim instanceof FakePlayer) return;

        boolean hasAttackerPvpDisabled = attacker.getPersistentData().getBoolean(TAG_PVP_DISABLED);
        boolean hasVictimPvpDisabled = victim.getPersistentData().getBoolean(TAG_PVP_DISABLED);

        if (hasAttackerPvpDisabled || hasVictimPvpDisabled) {
            cir.setReturnValue(false);
        }
    }
}
