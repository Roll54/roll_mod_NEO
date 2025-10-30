package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.mobEffect.SulfurPoisoningEffect;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MOB_EFFECT, RollMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> SULFUR_POISONING =
            EFFECTS.register("sulfur_poisoning", SulfurPoisoningEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
