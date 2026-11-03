package com.roll_54.roll_mod.gtmogs.integration.map;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.GTValues;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.integration.map.ftbchunks.FTBChunksRenderer;
import com.roll_54.roll_mod.gtmogs.integration.map.journeymap.JourneyMapRenderer;
import com.roll_54.roll_mod.gtmogs.integration.map.xaeros.XaerosRenderer;
import lombok.Getter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * A map renderer that groups together multiple map mods' renderers for supporting multiple simultaneous mods.
 */
public class GroupingMapRenderer extends GenericMapRenderer {

    @Getter
    private static final GroupingMapRenderer instance;

    static {
        Map<String, GenericMapRenderer> renderers = new HashMap<>();
        var toggle = ConfigHolder.INSTANCE.compat.minimap.toggle;

        if (toggle.journeyMapIntegration && GTMOGS.isModLoaded(GTValues.MODID_JOURNEYMAP)) {
            renderers.put(GTValues.MODID_JOURNEYMAP, new JourneyMapRenderer());
        }
        if (toggle.xaerosMapIntegration && GTMOGS.isModLoaded(GTValues.MODID_XAEROS_MINIMAP)) {
            renderers.put(GTValues.MODID_XAEROS_MINIMAP, new XaerosRenderer());
        }
        if (toggle.ftbChunksIntegration && GTMOGS.isModLoaded(GTValues.MODID_FTB_CHUNKS)) {
            renderers.put(GTValues.MODID_FTB_CHUNKS, new FTBChunksRenderer());
        }

        instance = new GroupingMapRenderer(renderers);
    }

    @Getter
    private final Map<String, GenericMapRenderer> renderers;
    private final GenericMapRenderer[] rendererList;

    public GroupingMapRenderer(Map<String, GenericMapRenderer> renderers) {
        super(false);
        this.renderers = renderers;
        this.rendererList = renderers.values().toArray(GenericMapRenderer[]::new);
    }


    @Override
    public boolean addMarker(String name, ResourceKey<Level> dim, GeneratedVeinMetadata vein, String id) {
        boolean value = false;
        for (GenericMapRenderer renderer : rendererList) {
            value |= renderer.addMarker(name, dim, vein, id);
        }
        return value;
    }

    @Override
    public boolean removeMarker(ResourceKey<Level> dim, String id) {
        boolean value = false;
        for (GenericMapRenderer renderer : rendererList) {
            value |= renderer.removeMarker(dim, id);
        }
        return value;
    }

    @Override
    public boolean doShowLayer(String name) {
        boolean value = false;
        for (GenericMapRenderer renderer : rendererList) {
            value |= renderer.doShowLayer(name);
        }
        return value;
    }

    @Override
    public void setLayerActive(String name, boolean active) {
        for (GenericMapRenderer renderer : rendererList) {
            renderer.setLayerActive(name, active);
        }
    }

    @Override
    public void clear() {
        for (GenericMapRenderer renderer : rendererList) {
            renderer.clear();
        }
    }
}
