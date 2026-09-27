package com.roll_54.roll_mod.minestar.letters;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A message from the staff to every player — or, when {@code recipient} is set, to that one player.
 *
 * <p><b>There is no recipient list.</b> A broadcast letter is for everyone who has ever joined and
 * everyone who joins later, until it expires: it is visible while {@link #visible} holds and unread for as
 * long as the reader is absent from {@code accepted}. {@code expiresAt} is therefore the only brake,
 * which is why the compose form defaults it to a week rather than to "never".
 *
 * <p>{@code body} keeps raw {@code &} colour codes — the mod's one markup convention, rendered with
 * {@code LegacyText.display} — and {@code \n} line breaks.
 *
 * @param sendAt    epoch millis it goes out; until then it is "due" and only staff see it. {@code 0}
 *                  (and every letter written before scheduling existed) means sent at creation
 * @param expiresAt epoch millis, or {@code 0} for never
 * @param icon      what it wears in the list, see {@link LetterIcon}; blank for the envelope
 * @param accepted  reader → when they accepted it
 * @param recipient the one player it is for, or {@code null} for everyone. Set only by
 *                  {@code LetterService.sendSystem} — the server's own notices, e.g. punishments
 */
public record Letter(UUID id, String title, String body, String author, long createdAt, long sendAt,
                     long expiresAt, List<LetterReward> rewards, String icon, Map<UUID, Long> accepted,
                     @Nullable UUID recipient) {

    /** A letter for everyone — what staff write. */
    public Letter(UUID id, String title, String body, String author, long createdAt, long sendAt,
                  long expiresAt, List<LetterReward> rewards, String icon, Map<UUID, Long> accepted) {
        this(id, title, body, author, createdAt, sendAt, expiresAt, rewards, icon, accepted, null);
    }

    public static final int MAX_TITLE = 64;
    public static final int MAX_BODY = 4000;
    public static final int MAX_REWARDS = 12;

    /** Readable by players: sent, and not yet expired. */
    public boolean visible(long now) {
        return !due(now) && !expired(now);
    }

    /** Scheduled and not out yet. */
    public boolean due(long now) {
        return sendAt > now;
    }

    public boolean expired(long now) {
        return expiresAt != 0L && now >= expiresAt;
    }

    /** The same letter with new content; who has accepted it, and when it was written, stay. */
    public Letter edited(String title, String body, long sendAt, long expiresAt, List<LetterReward> rewards,
                         String icon) {
        return new Letter(id, title, body, author, createdAt, sendAt, expiresAt, rewards, icon, accepted,
                recipient);
    }

    /** Whether {@code reader} is meant to see it: a broadcast, or addressed to them. */
    public boolean isFor(UUID reader) {
        return recipient == null || recipient.equals(reader);
    }

    public boolean acceptedBy(UUID reader) {
        return accepted.containsKey(reader);
    }
}
