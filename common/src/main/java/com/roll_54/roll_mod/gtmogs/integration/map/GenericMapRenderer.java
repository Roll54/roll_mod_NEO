package com.roll_54.roll_mod.gtmogs.integration.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import com.roll_54.roll_mod.gtmogs.integration.map.layer.Layers;
import com.roll_54.roll_mod.gtmogs.integration.map.layer.MapRenderLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A map renderer designed to work with any map mod.
 */
@OnlyIn(Dist.CLIENT)
public abstract class GenericMapRenderer {

    protected List<MapRenderLayer> layers;

    public GenericMapRenderer() {
        this(true);
    }

    public GenericMapRenderer(boolean initializeLayers) {
        layers = new ArrayList<>();
        if (initializeLayers) {
            Layers.addLayersTo(layers, this);
        }
    }

    public abstract boolean addMarker(String name, ResourceKey<Level> dim, GeneratedVeinMetadata vein, String id);

    public abstract boolean removeMarker(ResourceKey<Level> dim, String id);

    public abstract boolean doShowLayer(String name);

    public abstract void setLayerActive(String name, boolean active);

    public abstract void clear();
}
