package com.roll_54.roll_mod.minestar.chat;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The chat channels the server tags its messages with, so the client can sort them into the
 * channel buttons over the chat box. Anything the server sends untagged — the Discord bridge,
 * advancements, storm warnings, command feedback — is "other" and only shows under All.
 */
public enum ChatChannel {
    GLOBAL,
    LOCAL,
    FRIENDS,
    /** Server-wide buying and selling, apart from the general talk. Last: the packet sends the ordinal. */
    TRADE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("gui.roll_mod.chat.channel." + id());
    }
}
