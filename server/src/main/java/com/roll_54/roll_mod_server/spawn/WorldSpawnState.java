package com.roll_54.roll_mod_server.spawn;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persists the dimension a freshly-spawning / bedless-respawning player should be sent to.
 *
 * <p>Vanilla world spawn stores only a {@link net.minecraft.core.BlockPos} + angle (in the
 * overworld's {@code PrimaryLevelData}) and hard-codes the dimension to the overworld. We keep
 * reusing those vanilla coordinates — every non-overworld {@code ServerLevel} proxies them through
 * its {@code DerivedLevelData}, so {@code spawnDim.getSharedSpawnPos()} already returns the value
 * set by {@code /setworldspawn} — and only need to remember the <em>dimension</em> here.
 *
 * <p>Stored on the overworld's data storage; defaults to {@link Level#OVERWORLD} (= vanilla behaviour).
 */
public class WorldSpawnState extends SavedData {

    public static final String NAME = "roll_mod_world_spawn";

    public ResourceKey<Level> spawnDimension = Level.OVERWORLD;

    public static WorldSpawnState load(CompoundTag tag, HolderLookup.Provider provider) {
        WorldSpawnState s = new WorldSpawnState();
        if (tag.contains("dimension")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("dimension"));
            if (id != null) {
                s.spawnDimension = ResourceKey.create(Registries.DIMENSION, id);
            }
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString("dimension", spawnDimension.location().toString());
        return tag;
    }

    public static WorldSpawnState get(MinecraftServer server) {
        Factory<WorldSpawnState> factory =
                new Factory<>(WorldSpawnState::new, WorldSpawnState::load); // order: constructor, deserializer
        return server.overworld().getDataStorage().computeIfAbsent(factory, NAME);
    }
}