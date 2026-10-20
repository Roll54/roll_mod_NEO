package com.roll_54.roll_mod.minestar.teleport;

/**
 * Client copy of this player's running warm-up, from {@code TeleportCountdownPacket}: what the
 * hub's countdown window shows while the hub covers the action bar the server counts down on.
 */
public final class ClientTeleportCache {

    /** When the countdown runs out, in client {@code System.currentTimeMillis()}; 0 when none runs. */
    public static long ENDS_AT;
    /** Where it is going, as the server named it (legacy-formatted). */
    public static String DESTINATION = "";

    private ClientTeleportCache() {}

    public static boolean active() {
        return ENDS_AT > System.currentTimeMillis();
    }

    /** Whole seconds left, rounded up the way the action bar counts. */
    public static int secondsLeft() {
        long left = ENDS_AT - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) ((left + 999) / 1000);
    }
}
