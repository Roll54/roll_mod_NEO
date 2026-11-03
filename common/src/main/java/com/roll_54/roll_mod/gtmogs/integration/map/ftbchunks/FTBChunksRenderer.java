package com.roll_54.roll_mod.gtmogs.integration.map.ftbchunks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import com.roll_54.roll_mod.gtmogs.integration.map.GenericMapRenderer;
import com.roll_54.roll_mod.gtmogs.integration.map.ftbchunks.veins.ore.OreVeinIcon;
import dev.ftb.mods.ftblibrary.ui.Widget;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;

import java.util.Map;

public class FTBChunksRenderer extends GenericMapRenderer {

    public static final Table<ResourceKey<Level>, String, OreVeinIcon> oreElements = HashBasedTable.create();

    @Getter
    private static final Map<String, Widget> markers = new Object2ObjectOpenHashMap<>();

    @Override
    public boolean addMarker(String name, ResourceKey<Level> dim, GeneratedVeinMetadata vein, String id) {
        oreElements.put(dim, id, new OreVeinIcon(vein));
        return true;
    }

    @Override
    public boolean removeMarker(ResourceKey<Level> dim, String id) {
        var marker = oreElements.remove(dim, id);
        return marker != null;
    }

    @Override
    public boolean doShowLayer(String name) {
        return FTBChunksOptions.showLayer(name);
    }

    @Override
    public void setLayerActive(String name, boolean active) {
        FTBChunksOptions.toggleLayer(name, active);
    }

    @Override
    public void clear() {
        oreElements.clear();
    }
}
