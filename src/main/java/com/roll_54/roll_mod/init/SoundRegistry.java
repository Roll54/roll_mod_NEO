package com.roll_54.roll_mod.init;


import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, RollMod.MODID);

    public static final Supplier<SoundEvent> SHCHEDRYK = registerSoundEvent("shchedryk");
    public static final ResourceKey<JukeboxSong> SHCHEDRYK_KEY = createSong("shchedryk");

    public static final Supplier<SoundEvent> ROLL_CHIPUNK  = registerSoundEvent("roll_chipunk");

    public static final Supplier<SoundEvent> LORP_SUSPICIOUS_VIOLIN  = registerSoundEvent("lorp_suspicious_violin");





    private static ResourceKey<JukeboxSong> createSong(String name) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, name));
    }

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}