package com.roll_54.roll_mod.gtmogs.integration.map.cache.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import com.roll_54.roll_mod.gtmogs.integration.map.cache.DimensionCache;
import org.jetbrains.annotations.NotNull;

public class ServerCacheSavedData extends SavedData {

    public static final String DATA_NAME = "gtmogs_ore_vein_cache";

    private DimensionCache backingCache;
    private CompoundTag toRead;
    private HolderLookup.Provider toReadProvider;

    public static ServerCacheSavedData init(ServerLevel world, final DimensionCache backingCache) {
        ServerCacheSavedData instance = world.getDataStorage()
                .computeIfAbsent(new Factory<>(() -> new ServerCacheSavedData(backingCache),
                        (tag, registries) -> new ServerCacheSavedData(backingCache, tag, registries)),
                        DATA_NAME);

        instance.backingCache = backingCache;
        if (backingCache.dirty) {
            instance.setDirty();
        }
        if (instance.toRead != null) {
            backingCache.fromNBT(instance.toRead, instance.toReadProvider);
            instance.toRead = null;
            instance.toReadProvider = null;
        }

        return instance;
    }

    public ServerCacheSavedData(DimensionCache backingCache) {
        this.backingCache = backingCache;
    }

    public ServerCacheSavedData(DimensionCache backingCache,
                                CompoundTag compoundTag, HolderLookup.Provider registries) {
        this.backingCache = backingCache;
        if (backingCache != null) {
            backingCache.fromNBT(compoundTag, registries);
        } else {
            toRead = compoundTag;
            toReadProvider = registries;
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        return backingCache.toNBT(tag, registries);
    }
}
