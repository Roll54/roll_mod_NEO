package com.roll_54.roll_mod.minestar.hub.warp;

import java.util.List;

/**
 * The warp list as the client last received it, plus how many this player is allowed.
 *
 * <p>A plain static cache, like {@code ClientPlotCache} and {@code ClientAuctionCache}: the warp tab
 * reads it synchronously while building, and {@code SyncWarpsPacket} replaces it wholesale.
 */
public final class ClientWarpCache {

    public static volatile List<Warp> WARPS = List.of();

    /** How many warps the viewing player may own. Drives whether the create button is offered. */
    public static volatile int LIMIT = WarpService.DEFAULT_LIMIT;

    private ClientWarpCache() {}
}
