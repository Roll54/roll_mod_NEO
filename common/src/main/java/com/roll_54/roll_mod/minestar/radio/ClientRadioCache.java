package com.roll_54.roll_mod.minestar.radio;

import java.util.List;
import java.util.Map;

/**
 * The radio as this client last received it. {@link #INSTALLED} is what shows the hub's Radio tab;
 * it starts false and is reset on logout.
 */
public final class ClientRadioCache {

    /** One tunable station. */
    public record Station(String url, String title, String name) {}

    public static volatile boolean INSTALLED;
    public static volatile String STATION = "";
    public static volatile boolean HEAR = true;
    public static volatile boolean BROADCAST;
    public static volatile List<Station> STATIONS = List.of();

    /** Entity id → station url, for every player broadcasting to those around them. */
    public static volatile Map<Integer, String> BROADCASTS = Map.of();

    /** Bumped on every arrival. */
    public static volatile int VERSION;

    private ClientRadioCache() {}

    public static void clear() {
        INSTALLED = false;
        STATION = "";
        HEAR = true;
        BROADCAST = false;
        STATIONS = List.of();
        BROADCASTS = Map.of();
        VERSION++;
    }
}
