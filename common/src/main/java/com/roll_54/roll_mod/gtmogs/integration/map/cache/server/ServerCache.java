package com.roll_54.roll_mod.gtmogs.integration.map.cache.server;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import com.roll_54.roll_mod.gtmogs.common.network.packets.prospecting.SPacketProspectOre;
import com.roll_54.roll_mod.gtmogs.integration.map.cache.DimensionCache;
import com.roll_54.roll_mod.gtmogs.integration.map.cache.WorldCache;

import java.util.*;

public class ServerCache extends WorldCache {

    public static final ServerCache instance = new ServerCache();

    private final Map<ResourceKey<Level>, ServerCacheSavedData> saveData = new HashMap<>();

    public void maybeInitWorld(ServerLevel world) {
        ResourceKey<Level> dim = world.dimension();
        if (!cache.containsKey(dim)) {
            cache.put(dim, new DimensionCache());
        }
        if (!saveData.containsKey(dim)) {
            saveData.put(dim, ServerCacheSavedData.init(world, cache.get(dim)));
        }
    }

    public void invalidateWorld(ServerLevel world) {
        ResourceKey<Level> dim = world.dimension();
        cache.remove(dim);
        saveData.remove(dim);
    }

    @Override
    public boolean addVein(ResourceKey<Level> dim, int gridX, int gridZ, GeneratedVeinMetadata name) {
        boolean added = super.addVein(dim, gridX, gridZ, name);
        if (added && saveData.containsKey(dim)) {
            saveData.get(dim).setDirty();
        }
        return added;
    }

    @Override
    public void clear() {
        super.clear();
        saveData.clear();
    }

    public void prospectByOreMaterial(ResourceKey<Level> dim, BlockPos origin, ServerPlayer player,
                                      int radius) {
        if (radius < 0) return;
        List<GeneratedVeinMetadata> nearbyVeins = getNearbyVeins(dim, origin, radius);
        List<GeneratedVeinMetadata> foundVeins = new ArrayList<>();
        for (GeneratedVeinMetadata nearbyVein : nearbyVeins) {
            if (nearbyVein.definition().value().veinGenerator().getAllBlocks().contains(
                    Objects.requireNonNull(GTMOGS.getMinecraftServer().getLevel(dim)).getBlockState(origin))) {
                foundVeins.add(nearbyVein);
            }
        }
        PacketDistributor.sendToPlayer(player, new SPacketProspectOre(dim, foundVeins));
    }

    public void prospectByDepositName(ResourceKey<Level> dim, ResourceKey<OreVeinDefinition> veinId, BlockPos origin,
                                      ServerPlayer player,
                                      int radius) {
        if (radius < 0) return;
        List<GeneratedVeinMetadata> nearbyVeins = getNearbyVeins(dim, origin, radius);
        List<GeneratedVeinMetadata> foundVeins = new ArrayList<>();
        for (GeneratedVeinMetadata nearbyVein : nearbyVeins) {
            if (veinId.equals(nearbyVein.definition().getKey())) {
                foundVeins.add(nearbyVein);
            }
        }
        PacketDistributor.sendToPlayer(player, new SPacketProspectOre(dim, foundVeins));
    }

    public void prospectAllInChunk(ResourceKey<Level> dim, ChunkPos pos, ServerPlayer player) {
        List<GeneratedVeinMetadata> nearbyVeins = cache.get(dim).getVeinsInChunk(pos);
        List<GeneratedVeinMetadata> foundVeins = new ArrayList<>();
        Level level = GTMOGS.getMinecraftServer().getLevel(dim);
        for (GeneratedVeinMetadata nearbyVein : nearbyVeins) {
            if (cache.containsKey(dim)) {
                foundVeins.add(nearbyVein);
            }
        }
        PacketDistributor.sendToPlayer(player, new SPacketProspectOre(dim, foundVeins));
    }

    public void removeAllInChunk(ResourceKey<Level> dim, ChunkPos pos) {
        if (cache.containsKey(dim)) {
            cache.get(dim).removeAllInChunk(pos);
        }
    }
}
