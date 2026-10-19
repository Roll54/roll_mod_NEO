package com.roll_54.roll_mod.minestar.letters;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
 * @param dismissed readers who deleted it from their list after reading it. Only broadcasts use it:
 *                  a personal letter its recipient deletes is removed outright
 */
public record Letter(UUID id, String title, String body, String author, long createdAt, long sendAt,
                     long expiresAt, List<LetterReward> rewards, String icon, Map<UUID, Long> accepted,
                     @Nullable UUID recipient, Set<UUID> dismissed) {

    /** A letter for everyone — what staff write. */
    public Letter(UUID id, String title, String body, String author, long createdAt, long sendAt,
                  long expiresAt, List<LetterReward> rewards, String icon, Map<UUID, Long> accepted) {
        this(id, title, body, author, createdAt, sendAt, expiresAt, rewards, icon, accepted, null);
    }

    /** A letter nobody has deleted from their list yet. */
    public Letter(UUID id, String title, String body, String author, long createdAt, long sendAt,
                  long expiresAt, List<LetterReward> rewards, String icon, Map<UUID, Long> accepted,
                  @Nullable UUID recipient) {
        this(id, title, body, author, createdAt, sendAt, expiresAt, rewards, icon, accepted, recipient, Set.of());
    }

    public static final int MAX_TITLE = 64;
    public static final int MAX_BODY = 4000;
    public static final int MAX_REWARDS = 64;

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
                recipient, dismissed);
    }

    /** The same letter with {@code reader} also accepting it at {@code when}. */
    public Letter withAccepted(UUID reader, long when) {
        Map<UUID, Long> more = new java.util.HashMap<>(accepted);
        more.put(reader, when);
        return new Letter(id, title, body, author, createdAt, sendAt, expiresAt, rewards, icon,
                Map.copyOf(more), recipient, dismissed);
    }

    /** The same letter, gone from {@code reader}'s list. */
    public Letter withDismissed(UUID reader) {
        Set<UUID> more = new HashSet<>(dismissed);
        more.add(reader);
        return new Letter(id, title, body, author, createdAt, sendAt, expiresAt, rewards, icon, accepted,
                recipient, Set.copyOf(more));
    }

    public boolean dismissedBy(UUID reader) {
        return dismissed.contains(reader);
    }

    /** Whether {@code reader} is meant to see it: a broadcast, or addressed to them. */
    public boolean isFor(UUID reader) {
        return recipient == null || recipient.equals(reader);
    }

    public boolean acceptedBy(UUID reader) {
        return accepted.containsKey(reader);
    }
}
