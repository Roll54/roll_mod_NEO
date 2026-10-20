package com.roll_54.roll_mod.minestar.kits;

import java.util.List;

/**
 * Every kit and the display name it has, for the moderation tab's "kit names" window. Sent only to
 * those who may rename kits, so an empty list with {@link #ALLOWED} false is what everyone else has.
 */
public final class ClientKitNamesCache {

    public record Entry(String name, String displayName) {}

    public static volatile boolean ALLOWED;
    public static volatile List<Entry> KITS = List.of();
    /** Bumped on every sync, so the window can rebuild only when something arrived. */
    public static volatile int VERSION;

    private ClientKitNamesCache() {}
}
