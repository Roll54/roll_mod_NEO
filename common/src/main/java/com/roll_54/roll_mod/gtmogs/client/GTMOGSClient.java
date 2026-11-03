package com.roll_54.roll_mod.gtmogs.client;

import net.neoforged.bus.api.IEventBus;

/** GTMOGS's client side; started by {@link com.roll_54.roll_mod.RollMod} on the physical client only. */
public final class GTMOGSClient {

    private GTMOGSClient() {}

    public static void init(IEventBus modBus) {
        ClientInit.init(modBus);
    }
}
