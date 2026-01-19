package com.roll_54.roll_mod.mixin;

import com.roll_54.roll_mod.init.ModConfigs;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replacement of the vanilla armor absorption formula.
 * Parameters:
 *  - 0..10 armor -> linear up to 30% (3% per point)
 *  - 10..100+ -> asymptotically approaches 75% with rapidly diminishing returns
 * @author roll_54
 */
@Mixin(LivingEntity.class)
public abstract class ArmorReductionMixin {

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true)
    private void roll_mod$customArmorCurve(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {

        if (!ModConfigs.MAIN.pvp.enabled.get()) return;

        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player)) return;

        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return;

        int softCap = ModConfigs.MAIN.pvp.softCap.get();
        double maxReduction = ModConfigs.MAIN.pvp.maxReduction.get();
        double k = ModConfigs.MAIN.pvp.reductionCurve.get();

        double armor = Math.max(0.0, Math.min(100.0, self.getArmorValue()));

        double reduction;
        if (armor <= softCap) {
            reduction = 0.03 * armor;
        } else {
            double base = 0.30;
            reduction = base + (maxReduction - base) * (1.0 - Math.exp(-k * (armor - softCap)));

            if (reduction > maxReduction)
                reduction = maxReduction;
        }

        float customOut = (float)(amount * (1.0 - reduction));
        cir.setReturnValue(customOut);
    }
}
