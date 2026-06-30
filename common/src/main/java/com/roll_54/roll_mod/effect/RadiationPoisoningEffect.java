package com.roll_54.roll_mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import static com.roll_54.roll_mod.registry.DamageTypes.radiationDamageType;

/**
 * Visible "Radiation Poisoning" status effect. Its amplifier mirrors the radiation tier (0..5) computed
 * by {@code RadiationHandler}, and it deals escalating radiation damage over time, like
 * {@link SulfurPoisoningEffect}. The accompanying vanilla effects (nausea/poison/wither/...) are applied
 * by the handler per tier; this effect supplies the icon and the damage-over-time.
 */
public class RadiationPoisoningEffect extends MobEffect {

    public RadiationPoisoningEffect() {
        super(MobEffectCategory.HARMFUL, 0x3ad12a); // toxic green
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() > 1.0F) {
            entity.hurt(radiationDamageType(entity), 0.5F + amplifier);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int i = 40 >> amplifier;
        return i > 0 ? duration % i == 0 : true;
    }
}
