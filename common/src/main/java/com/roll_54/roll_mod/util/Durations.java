package com.roll_54.roll_mod.util;

/**
 * Turning a wait into something a player can read.
 *
 * <p>Cooldowns are held as ticks and stamped as epoch millis; both end up here before anyone sees
 * them, so a kit and a random teleport count down in the same words.
 */
public final class Durations {

    private Durations() {}

    /** {@code m:ss}, or {@code h:mm:ss} once there is an hour of it. Rounds up, never to "0:00". */
    public static String format(long millis) {
        long seconds = Math.max(0L, millis + 999L) / 1000L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long rest = seconds % 60L;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, rest)
                : String.format("%d:%02d", minutes, rest);
    }
}
