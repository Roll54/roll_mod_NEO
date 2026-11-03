package com.roll_54.roll_mod.gtmogs.integration.map.xaeros;


import com.roll_54.roll_mod.gtmogs.api.gui.misc.ProspectorMode;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import com.roll_54.roll_mod.gtmogs.integration.map.GenericMapRenderer;
import com.roll_54.roll_mod.gtmogs.integration.map.xaeros.minimap.ore.OreVeinElement;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class XaerosRenderer extends GenericMapRenderer {

    public static final Table<ResourceKey<Level>, String, OreVeinElement> oreElements = HashBasedTable.create();


    public XaerosRenderer() {
        super();
    }

    @Override
    public boolean addMarker(String name, ResourceKey<Level> dim, GeneratedVeinMetadata vein, String id) {
        oreElements.put(dim, id, new OreVeinElement(vein, name));
        return true;
    }

    @Override
    public boolean removeMarker(ResourceKey<Level> dim, String id) {
        OreVeinElement marker = oreElements.remove(dim, id);
        return marker != null;
    }

    @Override
    public boolean doShowLayer(String name) {
        return XaerosMapPlugin.getOptionValue(name);
    }

    @Override
    public void setLayerActive(String name, boolean active) {
        XaerosMapPlugin.toggleOption(name, active);
    }

    @Override
    public void clear() {
        oreElements.clear();
    }
}