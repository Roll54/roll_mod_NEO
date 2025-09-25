package com.roll_54.roll_mod.mixin;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Заміна ванільної формули поглинання бронею.
 * Параметри:
 *  - 0..10 armor  -> лінійно до 30% (3% за пункт)
 *  - 10..100+     -> асимптота 75% з різко спадним приростом
 * @author roll_54
 */
@Mixin(LivingEntity.class)
public abstract class ArmorReductionMixin {

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void roll_mod$customArmorCurve(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        double armor = Math.max(0.0, Math.min(100.0, self.getArmorValue())); // clamp до [0..100]

        double reduction;
        if (armor <= 10.0) {
            reduction = 0.03 * armor;
        } else {
            double k = 0.05;
            reduction = 0.30 + 0.45 * (1.0 - Math.exp(-k * (armor - 10.0)));
            if (reduction > 0.75) reduction = 0.75; // жорсткий кап
        }

        float out = (float) (amount * (1.0 - reduction));
        cir.setReturnValue(out);
    }
}