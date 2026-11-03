package com.roll_54.roll_mod.gtmogs.integration.map;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.GTValues;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.integration.map.ftbchunks.FTBChunksWaypointHandler;
import com.roll_54.roll_mod.gtmogs.integration.map.journeymap.JourneyMapWaypointHandler;
import com.roll_54.roll_mod.gtmogs.integration.map.xaeros.XaeroWaypointHandler;
import com.roll_54.roll_mod.gtmogs.utils.GTMath;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.HashSet;
import java.util.Set;

public class WaypointManager {

    public static ResourceKey<Level> currentDimension;

    @Getter
    private static boolean active = false;

    public static void init() {
        var toggle = ConfigHolder.INSTANCE.compat.minimap.toggle;

        if (toggle.xaerosMapIntegration && GTMOGS.isModLoaded(GTValues.MODID_XAEROS_MINIMAP)) {
            WaypointManager.registerWaypointHandler(new XaeroWaypointHandler());
            active = true;
        }
        if (toggle.journeyMapIntegration && GTMOGS.isModLoaded(GTValues.MODID_JOURNEYMAP)) {
            WaypointManager.registerWaypointHandler(new JourneyMapWaypointHandler());
            active = true;
        }

        if (toggle.ftbChunksIntegration && GTMOGS.isModLoaded(GTValues.MODID_FTB_CHUNKS)) {
            WaypointManager.registerWaypointHandler(new FTBChunksWaypointHandler());
            active = true;
        }
    }

    private static final Set<IWaypointHandler> handlers = new HashSet<>();
    private static final Object2ObjectMap<String, WaypointKey> waypoints = new Object2ObjectArrayMap<>();

    public static void updateDimension(LevelAccessor dim) {
        if (dim instanceof ClientLevel level) {
            currentDimension = level.dimension();
        }
    }

    public static void setWaypoint(String key, String name, int color, ResourceKey<Level> dim, int x, int y, int z) {
        if (dim == null) dim = currentDimension;
        for (IWaypointHandler handler : handlers) {
            handler.setWaypoint(key, name, color, dim, x, y, z);
        }
        waypoints.put(key, new WaypointKey(dim, x, y, z));
    }

    public static void removeWaypoint(String key) {
        for (IWaypointHandler handler : handlers) {
            handler.removeWaypoint(key);
        }
        waypoints.remove(key);
    }

    public static boolean toggleWaypoint(String key, String name, int color, ResourceKey<Level> dim, int x, int y,
                                         int z) {
        if (dim == null) dim = currentDimension;
        if ((new WaypointKey(dim, x, y, z)).equals(waypoints.get(key))) {
            removeWaypoint(key);
            return false;
        }
        setWaypoint(key, name, color, dim, x, y, z);
        return true;
    }

    public static void registerWaypointHandler(IWaypointHandler handler) {
        handlers.add(handler);
    }

    private static class WaypointKey {

        ResourceKey<Level> dim;
        int x, y, z;

        public WaypointKey(ResourceKey<Level> dim, int x, int y, int z) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WaypointKey that = (WaypointKey) o;
            return dim == that.dim && x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return GTMath.hashInts(dim.hashCode(), x, y, z);
        }
    }
}
