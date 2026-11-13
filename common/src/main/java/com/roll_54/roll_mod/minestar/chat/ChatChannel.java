package com.roll_54.roll_mod.minestar.chat;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The chat channels the server tags its messages with, so the client can sort them into the
 * channel buttons over the chat box. Anything the server sends untagged is sorted by the client
 * ({@code ClientChatStore#classify}): advancements, deaths and the rest of the server's notices go to
 * {@link #SPAM}, vanilla {@code /msg} to {@link #PM}, Discord
 * lines to {@link #GLOBAL}.
 *
 * <p>New channels go last: the packet sends the ordinal.
 */
public enum ChatChannel {
    GLOBAL,
    LOCAL,
    FRIENDS,
    /** Server-wide buying and selling, apart from the general talk. */
    TRADE,
    /** Private messages: {@code /pm} and vanilla {@code /msg}. */
    PM,
    /** Staff only: commands run on the server, what they answered, and KubeJS / roll_mod errors. */
    DEV,
    /** Advancements, deaths, joins, announcements and every other server notice. Read-only: commands only. */
    SPAM,
    /** The player's FTB Teams team: {@code /tm}, seen by its members only. */
    TEAM;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Players don't talk here; typing in its tab only runs commands. */
    public boolean readOnly() {
        return this == DEV || this == SPAM;
    }

    public Component title() {
        return Component.translatable("gui.roll_mod.chat.channel." + id());
    }
}
