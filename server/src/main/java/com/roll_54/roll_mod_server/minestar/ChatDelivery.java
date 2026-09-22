package com.roll_54.roll_mod_server.minestar;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hand-off between {@link ChatHandler} and {@code PlayerListChatMixin}.
 *
 * <p>This is the <em>global</em> channel's half of {@link ChatHandler}. A global message must not
 * cancel {@code ServerChatEvent}, otherwise every listener registered after it — other mods, chat
 * bridges, loggers — never sees it. Instead it is marked as already delivered here, and the mixin
 * drops the vanilla broadcast at the very last step, inside
 * {@code PlayerList#broadcastChatMessage}. The event itself completes uncancelled.
 *
 * <p>Local chat is the opposite case and never comes through here: it is cancelled outright, at
 * {@code HIGHEST} priority, precisely so that those same listeners never see it.
 *
 * <p>A message is marked only once its custom delivery actually succeeded, so any failure in
 * {@link ChatHandler} falls back to the untouched vanilla broadcast instead of silently
 * swallowing the message.
 *
 * <p>Counts are kept per player because the chat pipeline may queue several messages from the
 * same player between the event and the broadcast; ordering within one player's chain is
 * guaranteed by vanilla, so a counter is enough to pair them up.
 */
public final class ChatDelivery {

    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    private ChatDelivery() {}

    /** Marks the next pending broadcast from {@code sender} as already delivered by the mod. */
    public static void markHandled(ServerPlayer sender) {
        PENDING.merge(sender.getUUID(), 1, Integer::sum);
    }

    /**
     * {@return whether this broadcast was already delivered by the mod} Consumes the mark, so
     * each marked message suppresses exactly one vanilla broadcast.
     */
    public static boolean consumeHandled(ServerPlayer sender) {
        boolean[] handled = {false};
        PENDING.computeIfPresent(sender.getUUID(), (uuid, pending) -> {
            handled[0] = true;
            return pending > 1 ? pending - 1 : null;
        });
        return handled[0];
    }

    /** Drops any leftover marks for a disconnecting player. */
    public static void forget(UUID playerId) {
        PENDING.remove(playerId);
    }
}