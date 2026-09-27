package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.network.packet.SyncModerationPacket;

import java.util.List;

/**
 * The moderation tab's data as this client last received it.
 *
 * <p>{@link #ALLOWED} is what shows or hides the tab. It starts false and is reset on logout, so a
 * moderator who rejoins a server where they are not one does not see the bookmark flash up.
 */
public final class ClientModerationCache {

    public static volatile boolean ALLOWED;
    public static volatile boolean MAY_BAN;
    public static volatile boolean MAY_WRITE_LETTERS;
    public static volatile boolean WHITELIST;
    public static volatile String WHITELIST_MESSAGE = "";
    public static volatile List<Row> ROWS = List.of();

    /** Bumped on every arrival, so the tab can tell "new data" from "same data" cheaply. */
    public static volatile int VERSION;

    /**
     * A {@link ModerationRow} with its countdowns turned into local deadlines on arrival:
     * {@code -1} none, {@code 0} never ends, otherwise local epoch millis.
     */
    public record Row(ModerationRow row, long muteUntil, long banUntil) {

        public boolean muted(long now) {
            return muteUntil == 0L || (muteUntil > 0L && now < muteUntil);
        }

        public boolean banned(long now) {
            return banUntil == 0L || (banUntil > 0L && now < banUntil);
        }

        /** Millis left, {@code -1} when it never ends, {@code 0} once it has. */
        public static long left(long until, long now) {
            if (until == 0L) return -1L;
            return until < 0L ? 0L : Math.max(0L, until - now);
        }
    }

    private ClientModerationCache() {}

    public static void accept(SyncModerationPacket payload) {
        long now = System.currentTimeMillis();
        ROWS = payload.rows().stream()
                .map(r -> new Row(r, until(r.muteRemaining(), now), until(r.banRemaining(), now)))
                .toList();
        WHITELIST = payload.whitelist();
        WHITELIST_MESSAGE = payload.whitelistMessage();
        MAY_BAN = payload.mayBan();
        MAY_WRITE_LETTERS = payload.mayWriteLetters();
        ALLOWED = payload.allowed();
        VERSION++;
    }

    public static void clear() {
        accept(SyncModerationPacket.DENIED);
    }

    private static long until(long remaining, long now) {
        if (remaining < 0L) return -1L;
        return remaining == 0L ? 0L : now + remaining;
    }
}
