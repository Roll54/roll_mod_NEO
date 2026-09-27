package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * How fast the server is actually running, and who to tell when it is not.
 *
 * <p>Reads {@code MinecraftServer.getAverageTickTimeNanos()} rather than timing ticks here. That is
 * a rolling mean, which has a consequence worth knowing: a single spiked tick will not show up, and
 * the number lags a stall by a second or two. For an alert threshold that is the behaviour you
 * want — nobody should be pinged because one tick ran long — but it does mean this will not line up
 * exactly with a player's "the server lagged just now".
 *
 * <p>Sampled every second rather than every tick. The value is a rolling mean already, so twenty
 * reads a second would be twenty reads of nearly the same number.
 */
public final class TpsMonitor {

    /** Below this, the people who can do something about it get told. */
    public static final double LOW_TPS = 15.0;

    /** Yellow below this, red below {@link #LOW_TPS}. */
    public static final double FAIR_TPS = 18.0;

    private static final int SAMPLE_INTERVAL_TICKS = 20;

    /**
     * Long enough that a stall that lasts is reported more than once, short enough that a moderator
     * can see whether it is recovering. The {@code CrashReporter} cooldown, slowed down: a server
     * running at 8 TPS is one event, not one event a second.
     */
    private static final long ALERT_COOLDOWN_MS = 60_000L;

    private static final String LOG_FILE = "tps.log";
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static volatile double tps = 20.0;
    private static long lastAlert;

    private TpsMonitor() {}

    /** The latest reading. Safe to call from anywhere, including a UI binding's supplier. */
    public static double tps() {
        return tps;
    }

    /** Hundredths, for the sync binding — {@code 1987} means 19.87 TPS. */
    public static int tpsHundredths() {
        return (int) Math.round(tps * 100.0);
    }

    public static boolean low() {
        return tps < LOW_TPS;
    }

    /** Green, yellow or red, for whoever is drawing the number. */
    public static ChatFormatting colour(double value) {
        if (value < LOW_TPS) return ChatFormatting.RED;
        if (value < FAIR_TPS) return ChatFormatting.YELLOW;
        return ChatFormatting.GREEN;
    }

    /** One-decimal, the way every server tool writes it. */
    public static String format(double value) {
        return String.format("%.1f", value);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % SAMPLE_INTERVAL_TICKS != 0) return;

        long nanos = Math.max(1L, server.getAverageTickTimeNanos());
        // Capped at 20: the server does not run faster than its tick rate, it just finishes early,
        // so an idle server reporting 400 TPS would be noise rather than information.
        tps = Math.min(20.0, 1_000_000_000.0 / nanos);

        if (tps >= LOW_TPS) return;

        long now = System.currentTimeMillis();
        if (now - lastAlert < ALERT_COOLDOWN_MS) return;
        lastAlert = now;

        double mspt = nanos / 1_000_000.0;
        ModerationService.broadcastToModerators(server, Component.translatable(
                        "msg.roll_mod.moderation.tps.low", format(tps), String.format("%.1f", mspt))
                .withStyle(ChatFormatting.RED));
        RollMod.LOGGER.warn("[Moderation] TPS {} ({} ms/tick), {} player(s).",
                format(tps), String.format("%.1f", mspt), server.getPlayerCount());
        log(server, mspt);
    }

    /**
     * A line in {@code minestar/tps.log}, so a dip that happened at four in the morning is still
     * answerable in the afternoon. A failed write is swallowed — a full disk must not turn a lag
     * spike into a crash.
     */
    private static void log(MinecraftServer server, double mspt) {
        String line = "[%s] tps=%s mspt=%.1f players=%d%n".formatted(
                LocalDateTime.now().format(TIMESTAMP), format(tps), mspt, server.getPlayerCount());
        try {
            Files.writeString(MinestarFiles.resolve(LOG_FILE), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Moderation] could not write the TPS log", e);
        }
    }
}
