package com.roll_54.roll_mod.minestar.hub.home;

import net.minecraft.nbt.CompoundTag;

import java.time.Duration;
import java.util.UUID;

/**
 * One player's standing on one home: either invited and not yet answered, or let in.
 *
 * <p>Invites and memberships are the same record rather than two collections, so "a player appears
 * at most once per home" is a single invariant enforced in a single place, and accepting is a state
 * change rather than a move between lists.
 *
 * @param playerName the name the invite was addressed to, kept beside the id the way {@code
 *     Warp.ownerName} is — the owner's roster can then be drawn without a profile lookup, for
 *     accepted guests as well as pending ones. It goes stale after a name change, as that one does.
 * @param sentAt when the invite was sent — what an inbox is ordered by, and what
 *     {@link #isExpired(long)} measures against
 */
public record HomeShare(UUID player, String playerName, State state, long sentAt) {

    public enum State {
        PENDING,
        ACCEPTED
    }

    public static HomeShare pending(UUID player, String playerName) {
        return new HomeShare(player, playerName, State.PENDING, System.currentTimeMillis());
    }

    public HomeShare accepted() {
        return new HomeShare(player, playerName, State.ACCEPTED, sentAt);
    }

    public boolean isAccepted() {
        return state == State.ACCEPTED;
    }

    public boolean isPending() {
        return state == State.PENDING;
    }

    /**
     * How long an unanswered invite stands before it lapses.
     *
     * <p>An invite is a request for an answer, and one nobody answered in a day is not one they are
     * still thinking about. Letting them accumulate would also quietly eat the owner's
     * {@code PlayerHome.MAX_PENDING_INVITES} slots forever, so a single ignored invite could stop
     * them inviting anyone else to that home ever again.
     */
    public static final long INVITE_TTL_MILLIS = Duration.ofHours(24).toMillis();

    /**
     * Whether this is a pending invite that has run out of time. Accepted shares never lapse — that
     * is a membership, not an outstanding question.
     *
     * <p>Takes {@code now} so a single sweep judges every share against one instant, and so this is
     * testable without waiting a day.
     */
    public boolean isExpired(long now) {
        return isPending() && now - sentAt > INVITE_TTL_MILLIS;
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("player", player);
        tag.putString("playerName", playerName);
        // A boolean rather than the enum name: there are only ever two states, and this survives
        // the enum being renamed.
        tag.putBoolean("accepted", isAccepted());
        tag.putLong("sentAt", sentAt);
        return tag;
    }

    public static HomeShare load(CompoundTag tag) {
        return new HomeShare(
                tag.getUUID("player"),
                tag.getString("playerName"),
                tag.getBoolean("accepted") ? State.ACCEPTED : State.PENDING,
                tag.getLong("sentAt"));
    }
}
