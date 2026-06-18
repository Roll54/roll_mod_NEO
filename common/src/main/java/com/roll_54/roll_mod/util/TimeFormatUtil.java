package com.roll_54.roll_mod.util;

/**
 * Utility class for converting Minecraft ticks into readable time strings.
 * 1 second = 20 ticks
 */

public class TimeFormatUtil {

    /**
     * Converts ticks into a formatted time string like "1h 23m 5s".
     */
    public static String formatTime(int ticks) {
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        return seconds + "s";
    }

    /**
     * Converts ticks into a short format, e.g. "1:23:05" or "12:09".
     * Useful for HUDs or compact chat messages.
     */
    public static String formatShort(int ticks) {
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Converts ticks into total seconds (if you just need number math).
     */
    public static int toSeconds(int ticks) {
        return ticks / 20;
    }

    /**
     * Converts seconds back into ticks.
     */
    public static int toTicks(int seconds) {
        return seconds * 20;
    }
}
