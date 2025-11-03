package com.roll_54.roll_mod.init;


import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, RollMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ROLL_CHIPUNK =
            SOUND_EVENTS.register("chipunk",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "chipunk")
                    ));
}