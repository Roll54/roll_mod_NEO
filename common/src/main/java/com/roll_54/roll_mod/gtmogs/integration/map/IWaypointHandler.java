package com.roll_54.roll_mod.gtmogs.integration.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface IWaypointHandler {

    void setWaypoint(String key, String name, int color, ResourceKey<Level> dim, int x, int y, int z);

    void removeWaypoint(String key);
}
