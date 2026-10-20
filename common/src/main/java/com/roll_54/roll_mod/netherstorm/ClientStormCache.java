package com.roll_54.roll_mod.netherstorm;

/**
 * The client's copy of whether the Nether storm is raging, fed by {@link SyncStormPacket}.
 *
 * <p>Holds no client classes, so it is safe to load on a dedicated server. Read from chunk-meshing
 * threads (the portal model), hence volatile.
 */
public final class ClientStormCache {
    private ClientStormCache() {}

    private static volatile boolean active;

    public static boolean isActive() {
        return active;
    }

    public static void set(boolean value) {
        active = value;
    }
}
