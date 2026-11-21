package com.roll_54.roll_mod.util;

/**
 * Turning a wait into something a player can read.
 *
 * <p>Cooldowns are held as ticks and stamped as epoch millis; both end up here before anyone sees
 * them, so a kit and a random teleport count down in the same words.
 */
public final class Durations {

    /** One {@code <number><unit>} piece of what {@link #parse} accepts. */
    private static final java.util.regex.Pattern PART = java.util.regex.Pattern.compile("(\\d+)([smhdw])");

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

    /**
     * A wait in its largest units, the two biggest that apply: {@code 1 міс 3 д}, {@code 6 д 17 год},
     * {@code 20 год 27 хв}, {@code 27 хв 34 с}, {@code 34 с}. A month is counted as 30 days. Rounds up to the
     * second, never to nothing. Translated on the side that shows it.
     */
    public static net.minecraft.network.chat.Component readable(long millis) {
        long seconds = Math.max(1L, (Math.max(0L, millis) + 999L) / 1000L);
        long[] sizes = {30L * 86_400L, 86_400L, 3_600L, 60L, 1L};
        String[] keys = {"month", "day", "hour", "minute", "second"};
        net.minecraft.network.chat.MutableComponent out = null;
        int shown = 0;
        for (int i = 0; i < sizes.length && shown < 2; i++) {
            long n = seconds / sizes[i];
            if (n == 0 && out == null) continue;
            seconds -= n * sizes[i];
            if (n > 0) {
                net.minecraft.network.chat.Component part = net.minecraft.network.chat.Component.translatable("time.roll_mod.unit." + keys[i], n);
                out = out == null ? part.copy() : out.append(" ").append(part);
            }
            shown++;
        }
        return out == null ? net.minecraft.network.chat.Component.translatable("time.roll_mod.unit.second", 1) : out;
    }

    /**
     * The inverse of {@link #parse}: the largest units first, whole minutes and up — {@code 6d23h},
     * {@code 2w}, {@code 45m} — so a value loaded into an input field reads back as itself. {@code 0}
     * for nothing (which {@link #parse} reads as "no end"); anything under a minute rounds up to
     * {@code 1m} so a nearly-spent duration does not turn into "forever" on the way back.
     */
    public static String formatInput(long millis) {
        if (millis <= 0L) return "0";
        long minutes = Math.max(1L, (millis + 59_999L) / 60_000L);
        long[] units = {7L * 24L * 60L, 24L * 60L, 60L, 1L};
        String[] names = {"w", "d", "h", "m"};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < units.length; i++) {
            long n = minutes / units[i];
            if (n > 0) {
                out.append(n).append(names[i]);
                minutes -= n * units[i];
            }
        }
        return out.toString();
    }

    /**
     * What a moderator types for a mute or a ban: {@code 30m}, {@code 2h}, {@code 1d}, {@code 1w},
     * or several run together ({@code 1d12h}). A bare number is minutes, as {@code /mute} takes.
     *
     * @return the length in millis, {@code 0} for "no end" (typed as {@code 0}), or {@code -1}
     *         when the text does not parse — so a typo is refused rather than read as permanent
     */
    public static long parse(String text) {
        if (text == null) return -1L;
        String clean = text.strip().toLowerCase(java.util.Locale.ROOT);
        if (clean.isEmpty()) return -1L;
        if (clean.chars().allMatch(Character::isDigit)) {
            try {
                return Math.multiplyExact(Long.parseLong(clean), 60_000L);
            } catch (ArithmeticException | NumberFormatException e) {
                return -1L;
            }
        }
        java.util.regex.Matcher m = PART.matcher(clean);
        long total = 0L;
        int end = 0;
        while (m.find()) {
            if (m.start() != end) return -1L;
            end = m.end();
            long unit = switch (m.group(2)) {
                case "s" -> 1_000L;
                case "m" -> 60_000L;
                case "h" -> 3_600_000L;
                case "d" -> 86_400_000L;
                default -> 604_800_000L;
            };
            try {
                total = Math.addExact(total, Math.multiplyExact(Long.parseLong(m.group(1)), unit));
            } catch (ArithmeticException | NumberFormatException e) {
                return -1L;
            }
        }
        return end == clean.length() && end > 0 ? total : -1L;
    }
}
