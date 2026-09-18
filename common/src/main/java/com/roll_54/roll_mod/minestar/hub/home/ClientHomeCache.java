package com.roll_54.roll_mod.minestar.hub.home;

import java.util.List;

/**
 * What the client knows about homes. Written only by {@code SyncHomesPacket}, read only by {@code
 * PlayerHomesTab}. Mirrors {@code ClientWarpCache}, {@code ClientPlotCache} and {@code
 * ClientAuctionCache}.
 *
 * <p>Unlike the warp cache this is genuinely per-player: the server sends each viewer only the homes
 * they own or were invited to, already projected through their relation to each one.
 */
public final class ClientHomeCache {

    public static volatile List<HomeView> HOMES = List.of();
    public static volatile int LIMIT = HomeService.DEFAULT_LIMIT;

    /**
     * Names the invite dropdown offers: everyone this server has seen, minus the viewer themselves.
     * Already excludes nobody else — filtering out who is <em>on a given home</em> is per-home and
     * happens where the dropdown is built, against that home's roster.
     */
    public static volatile List<String> KNOWN_PLAYERS = List.of();

    private ClientHomeCache() {}
}
