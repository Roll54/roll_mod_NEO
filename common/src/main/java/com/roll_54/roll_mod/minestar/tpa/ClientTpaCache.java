package com.roll_54.roll_mod.minestar.tpa;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the tpa tab draws: this player's requests, and who else is online to send one to.
 *
 * <p>The roster comes from the server rather than from the client's own tab list, because the tab
 * is common code and cannot reach a client-only class.
 */
public final class ClientTpaCache {

    public static volatile List<TpaRequest> INCOMING = List.of();
    public static volatile List<TpaRequest> OUTGOING = List.of();

    /** Online players other than the viewer, id to name. */
    public static volatile Map<UUID, String> ONLINE = Map.of();

    /**
     * What this player currently lets through.
     *
     * <p>Server-owned: the settings window shows this and never sets it locally, so a click that
     * the server refuses never leaves the window claiming otherwise.
     */
    public static volatile TpaMode MODE = TpaMode.DEFAULT;

    private ClientTpaCache() {}
}
