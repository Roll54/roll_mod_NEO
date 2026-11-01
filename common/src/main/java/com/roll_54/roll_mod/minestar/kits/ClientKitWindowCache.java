package com.roll_54.roll_mod.minestar.kits;

/**
 * The layout of the kit window the server is about to open. It arrives just before the open and is
 * what the client's copy of the window builds from; see {@link KitWindowLayout}.
 */
public final class ClientKitWindowCache {

    public static volatile KitWindowLayout LAYOUT = KitWindowLayout.EMPTY;

    private ClientKitWindowCache() {}
}
