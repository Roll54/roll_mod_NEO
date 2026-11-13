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

    /** The kit last asked to preview, for the Kits tab's preview window; bumped on each arrival. */
    public static volatile com.roll_54.roll_mod.network.packet.KitWindowPackets.Contents PREVIEW;
    public static volatile int PREVIEW_VERSION;

    /** What is left of a kit's cooldown right now, rather than when the packet was built. */
    public static long remainingMillis(KitView kit) {
        // -1: a one-time kit already claimed; it does not count down.
        if (kit.remainingMillis() < 0) return -1L;
        return Math.max(0L, kit.remainingMillis() - (System.currentTimeMillis() - receivedAt));
    }

    private ClientKitCache() {}
}
