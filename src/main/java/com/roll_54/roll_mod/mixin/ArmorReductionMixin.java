package com.roll_54.roll_mod.mixin;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.roll_54.roll_mod.Config.GeneralConfig.CUSTOM_PVP;

/**
 * Replacement of the vanilla armor absorption formula.
 * Parameters:
 *  - 0..10 armor -> linear up to 30% (3% per point)
 *  - 10..100+ -> asymptotically approaches 75% with rapidly diminishing returns
 * @author roll_54
 */
@Mixin(LivingEntity.class)
public abstract class ArmorReductionMixin {

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true )
    private void roll_mod$customArmorCurve(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (CUSTOM_PVP.get()) {
            if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
                return;
            }
            LivingEntity self = (LivingEntity) (Object) this;
            double armor = Math.max(0.0, Math.min(100.0, self.getArmorValue())); // clamp [0..100]
            double reduction;
            if (armor <= 10.0) {
                reduction = 0.03 * armor;
            } else {
                double k = 0.05;
                reduction = 0.30 + 0.45 * (1.0 - Math.exp(-k * (armor - 10.0)));
                if (reduction > 0.75) reduction = 0.75;
            }
            float customOut = (float) (amount * (1.0 - reduction));
            cir.setReturnValue(customOut);
        }
        else {
            return;
        }
    }

}