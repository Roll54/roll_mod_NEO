package com.roll_54.roll_mod.minestar.moderation.invsee;

/**
 * The layout of the inventory window the server is about to open. It arrives just before the open
 * and is what the client's copy of the window builds from; see {@link InvSeeLayout}.
 */
public final class ClientInvSeeCache {

    public static volatile InvSeeLayout LAYOUT = InvSeeLayout.EMPTY;

    private ClientInvSeeCache() {}
}
