package com.roll_54.roll_mod.gtmogs.integration.map.journeymap;


import com.roll_54.roll_mod.gtmogs.GTMOGS;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.option.BooleanOption;
import journeymap.api.v2.client.option.OptionCategory;

import java.util.HashMap;
import java.util.Map;

public class JourneyMapOptions {

    private final Map<String, BooleanOption> layerOptions = new HashMap<>();

    public JourneyMapOptions() {
        final String prefix = "gtmogs.journeymap.options.layers.";
        final OptionCategory category = new OptionCategory(GTMOGS.MOD_ID, "gtmogs.journeymap.options.layers");

        final BooleanOption oreLayer = new BooleanOption(category, "ore_veins", prefix + "ore_veins", false);
        layerOptions.put(oreLayer.getFieldName(), oreLayer);
    }

    public boolean showLayer(String name) {
        return layerOptions.get(name).get();
    }

    public void toggleLayer(String name, boolean active) {
        layerOptions.get(name).set(active);
        if (!active) {
            JourneyMapRenderer.getMarkers().forEach((id, marker) -> {
                if (id.split("@")[0].equals(name)) {
                    IClientAPI api = JourneyMapPlugin.getJmApi();
                    api.remove(marker);
                }
            });
        } else {
            JourneyMapRenderer.getMarkers().forEach((id, marker) -> {
                if (id.split("@")[0].equals(name)) {
                    try {
                        IClientAPI api = JourneyMapPlugin.getJmApi();
                        api.show(marker);
                    } catch (Exception e) {
                        // It never actually throws anything...
                        GTMOGS.LOGGER.error("Failed to enable marker with name {}", name, e);
                    }
                }
            });
        }
    }
}