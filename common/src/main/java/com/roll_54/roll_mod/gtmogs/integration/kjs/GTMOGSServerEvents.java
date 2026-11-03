package com.roll_54.roll_mod.gtmogs.integration.kjs;

import com.roll_54.roll_mod.gtmogs.integration.kjs.events.GTOreVeinKubeEvent;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface GTMOGSServerEvents {

    EventGroup GROUP = EventGroup.of("GTMOGSServerEvents");

    EventHandler ORE_VEIN_MODIFICATION = GROUP.server("oreVeins", () -> GTOreVeinKubeEvent.class);
}
