package com.roll_54.roll_mod.init;


import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, RollMod.MODID);

    public static final Supplier<SoundEvent> ROLL_CHIPUNK = regissterSoundEvent("chipunk");



    public static Supplier<SoundEvent> regissterSoundEvent(String name){
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }


    public static void register(IEventBus eventBus) {
SOUND_EVENTS.register(eventBus);
    }

}