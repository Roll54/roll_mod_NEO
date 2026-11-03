package com.roll_54.roll_mod.gtmogs.integration.map.layer;

import com.roll_54.roll_mod.gtmogs.integration.map.GenericMapRenderer;

public abstract class MapRenderLayer {

    protected final String key;
    protected final GenericMapRenderer renderer;

    public MapRenderLayer(String key, GenericMapRenderer renderer) {
        this.key = key;
        this.renderer = renderer;
    }
}
