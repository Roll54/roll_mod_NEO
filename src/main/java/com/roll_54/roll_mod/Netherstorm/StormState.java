package com.roll_54.roll_mod.Netherstorm;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class StormState extends SavedData {
    public static final String NAME = "nether_storm";

    public boolean stormActive = false;
    public int stormTicks = 0;
    public int stormDuration = 0;
    public int ticksUntilNextStorm = 0;

    public static StormState load(CompoundTag tag, HolderLookup.Provider provider) {
        StormState s = new StormState();
        s.stormActive = tag.getBoolean("stormActive");
        s.stormTicks = tag.getInt("stormTicks");
        s.stormDuration = tag.getInt("stormDuration");
        s.ticksUntilNextStorm = tag.getInt("ticksUntilNextStorm");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("stormActive", stormActive);
        tag.putInt("stormTicks", stormTicks);
        tag.putInt("stormDuration", stormDuration);
        tag.putInt("ticksUntilNextStorm", ticksUntilNextStorm);
        return tag;
    }

    public static StormState get(ServerLevel overworld) {
        Factory<StormState> factory =
                new Factory<>(StormState::new, StormState::load); // ПОРЯДОК: constructor, deserializer

        return overworld.getDataStorage().computeIfAbsent(factory, NAME);
    }

    public void dirty() { setDirty(); }
}
