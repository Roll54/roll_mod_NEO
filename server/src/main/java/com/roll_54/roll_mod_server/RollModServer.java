package com.roll_54.roll_mod_server;

import com.roll_54.roll_mod.RollMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Entry point for the dedicated-server jar. All server features register themselves
 * through {@code @EventBusSubscriber(modid = RollModServer.MODID)} game-bus handlers,
 * so the constructor only needs to exist.
 */
@Mod(value = RollModServer.MODID, dist = Dist.DEDICATED_SERVER)
public final class RollModServer {
    public static final String MODID = "roll_mod_server";

    public RollModServer(ModContainer container) {
        RollMod.LOGGER.info("[{}] init complete.", MODID);
    }
}
