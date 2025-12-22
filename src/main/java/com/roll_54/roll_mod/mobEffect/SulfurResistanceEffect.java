package com.roll_54.roll_mod.mobEffect;

import com.roll_54.roll_mod.init.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import static com.roll_54.roll_mod.init.ModEffects.SULFUR_POISONING;
import static com.roll_54.roll_mod.init.ModEffects.SULFUR_RESISTANCE;

public class SulfurResistanceEffect extends MobEffect {

    public SulfurResistanceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x70d53a);
    }



    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {

        // If the entity has sulfur resistance, remove sulfur poisoning
        if (entity.hasEffect(SULFUR_RESISTANCE)) {
            entity.removeEffect(SULFUR_POISONING); // removes sulfur poisoning
            return true; // stop further ticking
        }

        // Poison-like damage that cannot kill
        if (entity.getHealth() > 1.0F) {
            entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // частота шкоди (як у зілля отрути)
        int i = 40 >> amplifier;
        return i > 0 ? duration % i == 0 : true;
    }


}
