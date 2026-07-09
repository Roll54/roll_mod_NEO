package com.roll_54.roll_mod_server.crash;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports a caught server-side runtime exception to the console and to online operators in chat,
 * instead of letting it hard-crash the dedicated server. Identical exceptions are rate-limited so a
 * per-tick failure doesn't spam chat or lock the server.
 */
public final class CrashReporter {
    private CrashReporter() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long COOLDOWN_MS = 10_000L;
    private static final int MAX_HOVER_CHARS = 4000;

    private static final ConcurrentHashMap<String, Long> LAST_REPORT = new ConcurrentHashMap<>();

    /** Log the throwable and, if not rate-limited, broadcast a summary (with full-trace hover) to ops. */
    public static void report(MinecraftServer server, Throwable t) {
        // Always log the full trace to the console.
        LOGGER.error("[roll_mod] Caught server exception (suppressed to keep the server alive)", t);

        String signature = signatureOf(t);
        long now = System.currentTimeMillis();
        Long last = LAST_REPORT.get(signature);
        if (last != null && now - last < COOLDOWN_MS) {
            return; // recently reported this exact failure; skip chat spam
        }
        LAST_REPORT.put(signature, now);

        if (server == null) {
            return;
        }

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String fullTrace = sw.toString();
        if (fullTrace.length() > MAX_HOVER_CHARS) {
            fullTrace = fullTrace.substring(0, MAX_HOVER_CHARS) + "\n… (truncated, see server log)";
        }

        Component hover = Component.literal(fullTrace);
        MutableComponent message = Component.literal("[roll_mod] Server exception (suppressed): ")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal(String.valueOf(t))
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GRAY)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))))
                .append(Component.literal(" (hover for trace)").withStyle(ChatFormatting.DARK_GRAY));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static String signatureOf(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        String top = trace.length > 0 ? trace[0].toString() : "no-frame";
        return t.getClass().getName() + '|' + t.getMessage() + '|' + top;
    }
}
