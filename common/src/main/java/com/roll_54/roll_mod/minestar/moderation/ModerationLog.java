package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An append-only record of every punishment, next to {@code op_users_check.txt}.
 *
 * <p>The stores hold what is <em>true now</em> — who is banned, how many warnings someone has. This
 * holds what <em>happened</em>, including the things the stores forget: a ban that has since
 * expired, a mute that was lifted, a warning cleared by an operator. When a player asks why they
 * were punished six weeks ago, the stores cannot answer and this can.
 *
 * <p>Never read back by the code, so it is plain text rather than JSON, and a failed write is
 * logged and swallowed — losing an audit line must not fail the punishment that was being recorded.
 */
public final class ModerationLog {

    private static final String FILE = "moderation.log";
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ModerationLog() {}

    /**
     * One line: when, who did it, what they did, to whom, and why.
     *
     * @param actor the moderator's name, or {@code "server"} for something automatic
     */
    public static void append(String actor, String action, String target, String detail) {
        StringBuilder line = new StringBuilder("[")
                .append(LocalDateTime.now().format(TIMESTAMP))
                .append("] ").append(actor).append(' ').append(action);
        // Both are optional: closing the server has no target, and an unban has no detail.
        if (target != null && !target.isBlank()) line.append(' ').append(target);
        if (detail != null && !detail.isBlank()) line.append(" — ").append(detail);
        line.append(System.lineSeparator());
        try {
            Files.writeString(MinestarFiles.resolve(FILE), line.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Moderation] could not write the audit log", e);
        }
    }
}
