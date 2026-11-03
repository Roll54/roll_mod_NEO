package com.roll_54.roll_mod.gtmogs.integration.map.cache.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import com.roll_54.roll_mod.gtmogs.integration.map.GenericMapRenderer;
import com.roll_54.roll_mod.gtmogs.integration.map.GroupingMapRenderer;
import com.roll_54.roll_mod.gtmogs.integration.map.cache.DimensionCache;
import com.roll_54.roll_mod.gtmogs.integration.map.cache.GridCache;
import com.roll_54.roll_mod.gtmogs.integration.map.cache.WorldCache;
import com.roll_54.roll_mod.gtmogs.integration.map.layer.builtin.OreRenderLayer;

import java.util.Collection;

public class GTClientCache extends WorldCache implements IClientCache {

    public static final GTClientCache instance = new GTClientCache();

    public void notifyNewVeins(GeneratedVeinMetadata... veins) {
        if (veins.length == 0) return;

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) return;

        for (var vein : veins) {
            var veinId = vein.definition().getKey().location();
            var name = Component.translatable(veinId.toLanguageKey("ore_vein"));
            player.sendSystemMessage(Component.translatable("message.gtmogs.new_veins.name", name));
        }
    }

    @Override
    public boolean addVein(ResourceKey<Level> dim, int gridX, int gridZ, GeneratedVeinMetadata vein) {
        GenericMapRenderer renderer = GroupingMapRenderer.getInstance();
        if (renderer != null) {
            renderer.addMarker(OreRenderLayer.getName(vein).getString(), dim, vein, OreRenderLayer.getId(vein));
        }
        boolean added = super.addVein(dim, gridX, gridZ, vein);
        if (added) {
            notifyNewVeins(vein);
        }
        return added;
    }

    @Override
    public Collection<ResourceKey<Level>> getExistingDimensions(String prefix) {
        return cache.keySet();
    }

    @Override
    public CompoundTag saveDimFile(String prefix, ResourceKey<Level> dim, HolderLookup.Provider registries) {
        if (!cache.containsKey(dim)) return null;
        return cache.get(dim).toNBT(registries);
    }

    @Override
    public CompoundTag saveSingleFile(String name, HolderLookup.Provider registries) {
        return new CompoundTag();
    }

    @Override
    public void readDimFile(String prefix, ResourceKey<Level> dim, CompoundTag data, HolderLookup.Provider registries) {
        if (!cache.containsKey(dim)) {
            cache.put(dim, new DimensionCache());
        }
        cache.get(dim).fromNBT(data, registries);

        // FIXME janky hack mate
        GenericMapRenderer renderer = GroupingMapRenderer.getInstance();
        if (renderer != null) {
            for (GridCache grid : cache.get(dim).getCache().values()) {
                for (GeneratedVeinMetadata vein : grid.getVeins()) {
                    renderer.addMarker(OreRenderLayer.getName(vein).getString(), dim, vein, OreRenderLayer.getId(vein));
                }
            }
        }
    }

    @Override
    public void readSingleFile(String name, CompoundTag data, HolderLookup.Provider registries) {}

    @Override
    public void setupCacheFiles() {
        addDimFiles();
        addSingleFile("fluids");
    }

    @Override
    public void clear() {
        super.clear();
    }
}
