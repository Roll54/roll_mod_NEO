package com.roll_54.roll_mod.gtmogs.integration.kjs;

import com.roll_54.roll_mod.gtmogs.integration.kjs.events.WorldGenLayerKubeEvent;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface GTMOGSStartupEvents {

    EventGroup GROUP = EventGroup.of("GTMOGSStartupEvents");

    EventHandler WORLD_GEN_LAYERS = GROUP.startup("worldGenLayers", () -> WorldGenLayerKubeEvent.class);
}
