package com.roll_54.roll_mod.minestar.kits;

import java.util.List;

/**
 * The kits the server last told this client about.
 *
 * <p>Lives in {@code common} rather than the client jar because the hub tab that reads it is common
 * code — the same reason {@code ClientWarpCache} does.
 */
public final class ClientKitCache {

    public static volatile List<KitView> KITS = List.of();

    /**
     * When the list arrived, so the tab can age the cooldowns between syncs.
     *
     * <p>Without it a countdown would sit still until the next packet, and the server has no reason
     * to send one every tick just to move a number.
     */
    public static volatile long receivedAt;

    /** What is left of a kit's cooldown right now, rather than when the packet was built. */
    public static long remainingMillis(KitView kit) {
        return Math.max(0L, kit.remainingMillis() - (System.currentTimeMillis() - receivedAt));
    }

    private ClientKitCache() {}
}
