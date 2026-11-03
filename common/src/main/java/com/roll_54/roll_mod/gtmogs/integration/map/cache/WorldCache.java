package com.roll_54.roll_mod.gtmogs.integration.map.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class WorldCache {

    /**
     * Concurrent because {@code OreGenerator} adds veins from the worldgen worker threads while the server thread
     * reads this for prospecting, saving and reloads. A plain {@link HashMap} resizing under a concurrent write
     * can leave a reader spinning indefinitely, which surfaces as a server that never recovers its tick rate.
     */
    protected final Map<ResourceKey<Level>, DimensionCache> cache = new ConcurrentHashMap<>();

    public boolean addVein(ResourceKey<Level> dim, int gridX, int gridZ, GeneratedVeinMetadata vein) {
        return cache.computeIfAbsent(dim, d -> new DimensionCache()).addVein(gridX, gridZ, vein);
    }

    public List<GeneratedVeinMetadata> getNearbyVeins(ResourceKey<Level> dim, BlockPos pos, int blockRadius) {
        if (cache.containsKey(dim)) {
            return cache.get(dim).getNearbyVeins(pos, blockRadius);
        }
        return new ArrayList<>();
    }

    public List<GeneratedVeinMetadata> getVeinsInArea(ResourceKey<Level> dim, int[] bounds) {
        if (cache.containsKey(dim)) {
            return cache.get(dim).getVeinsInBounds(
                    new BlockPos(bounds[0], 0, bounds[1]),
                    new BlockPos(bounds[0] + bounds[2], 0, bounds[1] + bounds[3]));
        }
        return new ArrayList<>();
    }

    public void clear() {
        cache.clear();
    }

    public void oreVeinDefinitionsChanged(Registry<OreVeinDefinition> registry) {
        // Existing instances of vein definitions referenced by the cache are now invalid. Repopulate them here.
        for (DimensionCache levelCache : cache.values()) {
            for (GridCache gridCache : levelCache.getCache().values()) {
                gridCache.refreshDefinitions(registry);
            }
        }
    }
}
