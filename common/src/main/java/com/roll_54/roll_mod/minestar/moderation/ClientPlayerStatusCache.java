package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.network.packet.SyncPlayerStatusPacket;
import com.roll_54.roll_mod.util.Durations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * This client's own standing, as last sent by {@code ModerationStatus}.
 *
 * <p>A cache rather than a UI binding because the inventory's hub button reads it too, and bindings
 * only exist while the hub screen is open. The home tab reads it directly for the same reason,
 * which also keeps its binding list — and therefore every sync id after it — where it was.
 */
public final class ClientPlayerStatusCache {

    public static volatile int WARNS;
    public static volatile int UNREAD_LETTERS;
    public static volatile boolean MODERATOR;
    /** Daily tasks finished but not collected, plus one for an uncollected bonus. */
    public static volatile int DAILY_CLAIMABLE;

    /** Local epoch millis the mute ends; {@code -1} not muted, {@code 0} never ends. */
    private static volatile long muteUntil = -1L;
    /**
     * The {@link #muteUntil} the player has already looked at on the home tab; the badge is for a
     * mute they have not seen yet, not for the whole of it.
     */
    private static volatile long seenMuteUntil = Long.MIN_VALUE;
    /**
     * How far two computed end times may drift and still be the same mute. Every status packet
     * recomputes the end from "now + remaining", so the same mute lands a few millis off each time.
     */
    private static final long SAME_MUTE_SLACK = 2000L;

    private ClientPlayerStatusCache() {}

    public static void accept(SyncPlayerStatusPacket payload) {
        WARNS = payload.warns();
        UNREAD_LETTERS = payload.unreadLetters();
        MODERATOR = payload.moderator();
        DAILY_CLAIMABLE = payload.dailyClaimable();
        long remaining = payload.muteRemaining();
        long until = remaining < 0L ? -1L
                : remaining == 0L ? 0L
                : System.currentTimeMillis() + remaining;
        // A resync of the mute already known keeps its end time, so it stays "seen".
        long old = muteUntil;
        boolean same = until == old || (until > 0L && old > 0L && Math.abs(until - old) <= SAME_MUTE_SLACK);
        if (!same) muteUntil = until;
    }

    /** Called on logout, so nothing from one server leaks onto the next. */
    public static void clear() {
        WARNS = 0;
        UNREAD_LETTERS = 0;
        MODERATOR = false;
        DAILY_CLAIMABLE = 0;
        muteUntil = -1L;
        seenMuteUntil = Long.MIN_VALUE;
    }

    public static boolean muted() {
        long until = muteUntil;
        return until == 0L || (until > 0L && System.currentTimeMillis() < until);
    }

    /** Remaining mute in millis, {@code -1} when it never ends, {@code 0} when not muted. */
    public static long muteRemaining() {
        long until = muteUntil;
        if (until == 0L) return -1L;
        return until < 0L ? 0L : Math.max(0L, until - System.currentTimeMillis());
    }

    /** Muted, and the player has not opened the home tab since this mute came in. */
    public static boolean muteUnseen() {
        return muted() && muteUntil != seenMuteUntil;
    }

    /** The home tab is on screen: the mute shown there is no longer news. */
    public static void acknowledgeStanding() {
        seenMuteUntil = muteUntil;
    }

    /** Anything the hub button should draw attention to. */
    public static boolean attention() {
        return UNREAD_LETTERS > 0 || DAILY_CLAIMABLE > 0 || muteUnseen();
    }

    /** Why {@link #attention} is set, one line each, for the hub button's tooltip. */
    public static List<Component> alertLines() {
        List<Component> lines = new ArrayList<>();
        if (UNREAD_LETTERS > 0) {
            lines.add(Component.translatable("gui.roll_mod.hub.status.letters", UNREAD_LETTERS)
                    .withStyle(ChatFormatting.GOLD));
        }
        if (DAILY_CLAIMABLE > 0) {
            lines.add(Component.translatable("gui.roll_mod.hub.status.daily", DAILY_CLAIMABLE)
                    .withStyle(ChatFormatting.GREEN));
        }
        if (muted()) {
            long remaining = muteRemaining();
            lines.add(remaining < 0L
                    ? Component.translatable("gui.roll_mod.hub.status.mutedForever")
                            .withStyle(ChatFormatting.RED)
                    : Component.translatable("gui.roll_mod.hub.status.muted",
                            Durations.format(remaining)).withStyle(ChatFormatting.RED));
        }
        if (WARNS > 0) {
            lines.add(Component.translatable("gui.roll_mod.hub.status.warns", WARNS,
                    ModerationService.WARNS_BEFORE_BAN).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
