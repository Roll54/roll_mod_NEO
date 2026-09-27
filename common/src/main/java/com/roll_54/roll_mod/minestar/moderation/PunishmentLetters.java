package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.minestar.letters.LetterService;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.ServerLang;
import net.minecraft.server.MinecraftServer;

import java.util.Locale;
import java.util.UUID;

/**
 * The letter a punished player finds in their hub: what they got, for how long, which rules, the
 * moderator's note, and where an appeal goes. Sent for every mute, warning and ban — from the
 * moderation tab, from the commands, and for the automatic ban after the third warning — so the
 * player keeps a written record rather than a chat line that scrolls away, or a ban screen they
 * see once.
 *
 * <p>In Ukrainian, resolved on the server through {@link ServerLang}: a letter's text is stored, not
 * translated per reader.
 */
public final class PunishmentLetters {

    /** Drawn by {@code LetterIcons}; until the PNG exists it falls back to the envelope. */
    public static final String HAMMER_ICON = "roll_mod:textures/gui/hub/letters/hammer.png";

    /** Long enough to outlast the punishment for anyone who is banned or away when it is sent. */
    private static final long LIFETIME = 30L * 24L * 60L * 60L * 1000L;

    private static final String KEY = "letters.roll_mod.punishment.";

    public enum Kind {
        MUTE, WARN, BAN;

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private PunishmentLetters() {}

    /**
     * @param rules    comma-joined rule ids as stored ({@code "1.2,2.5"}), or blank
     * @param millis   the punishment's length; {@code <= 0} is forever. Ignored for a warning
     * @param warnings the player's warning total, for a warning; ignored otherwise
     */
    public static void send(MinecraftServer server, UUID target, Kind kind, String moderator,
                            long millis, String rules, String note, int warnings) {
        StringBuilder body = new StringBuilder();
        String term = millis <= 0L ? ServerLang.uk(KEY + "forever") : Durations.formatInput(millis);
        body.append(kind == Kind.WARN
                ? ServerLang.uk(KEY + "warn.body", warnings, ModerationService.WARNS_BEFORE_BAN)
                : ServerLang.uk(KEY + kind.key() + ".body", term));
        body.append('\n').append(ServerLang.uk(KEY + "by", moderator));

        body.append("\n\n&6").append(ServerLang.uk(KEY + "reasons")).append("&r");
        boolean any = false;
        for (String id : RulesStore.ids(rules)) {
            any = true;
            body.append("\n&e").append(id).append("&r — ").append(ServerLang.uk("rule.roll_mod." + id));
        }
        if (!any) body.append('\n').append(ServerLang.uk(KEY + "noReason"));
        if (note != null && !note.isBlank()) {
            body.append("\n\n").append(ServerLang.uk(KEY + "note", note.strip()));
        }

        body.append("\n\n&c").append(ServerLang.uk(KEY + "appeal"));

        LetterService.sendSystem(server, target, ServerLang.uk(KEY + "author"),
                ServerLang.uk(KEY + kind.key() + ".title"), body.toString(), HAMMER_ICON, LIFETIME);
    }
}
