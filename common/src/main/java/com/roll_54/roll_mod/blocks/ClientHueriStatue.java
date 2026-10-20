package com.roll_54.roll_mod.blocks;

/**
 * This client's copy of {@code RMMAttachment.HUERI_STATUE_SEEN}, as {@code HueriStatueSeenPacket}
 * last set it. Plain static state with no client imports, so the block can read it from
 * {@code animateTick} without the dedicated server ever loading a client class.
 *
 * <p>{@code true} until the server says otherwise: a statue that sparkles for a moment on join,
 * before the packet lands, for a player who read it long ago, would be worse than one that starts
 * sparkling a moment late.
 */
public final class ClientHueriStatue {

    public static volatile boolean seen = true;

    /**
     * The players the statue's text names, as the server last described them — filled just before
     * each open ({@link HueriStatuePlayers}), kept for the session.
     */
    public static final java.util.Map<java.util.UUID, HueriStatuePlayers.Entry> PLAYERS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private ClientHueriStatue() {}
}
