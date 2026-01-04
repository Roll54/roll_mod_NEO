package com.roll_54.roll_mod.modItems;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

public record InjectionEffect(
        Holder<MobEffect> effect,
        int durationTicks,
        int amplifier
) {}