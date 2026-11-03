package com.roll_54.roll_mod.gtmogs.integration.map.layer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.roll_54.roll_mod.gtmogs.integration.map.ButtonState;
import com.roll_54.roll_mod.gtmogs.integration.map.GenericMapRenderer;

import java.util.*;
import java.util.function.BiFunction;

public class Layers {

    private static final BiMap<String, BiFunction<String, GenericMapRenderer, ? extends MapRenderLayer>> layers = HashBiMap
            .create();

    public static void registerLayer(BiFunction<String, GenericMapRenderer, ? extends MapRenderLayer> initFunction,
                                     String key) {
        layers.put(key, initFunction);
        ButtonState.Button.makeButton(key);
    }

    public static void addLayersTo(List<MapRenderLayer> layers, GenericMapRenderer renderer) {
        for (var layer : Layers.layers.entrySet()) {
            layers.add(layer.getValue().apply(layer.getKey(), renderer));
        }
    }

    public static Collection<String> allKeys() {
        return layers.keySet();
    }
}
