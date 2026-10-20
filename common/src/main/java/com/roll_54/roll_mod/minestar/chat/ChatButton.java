package com.roll_54.roll_mod.minestar.chat;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * One button over the chat box: a built-in channel, or a channel of the player's own.
 *
 * <p>A built-in button picks both what the chat shows and where messages go. A custom one picks
 * where messages go and shows All — its {@code template} is what is sent, {@code {msg}} standing for the
 * typed text (or the text is appended after a space when the template has no {@code {msg}}). A
 * template starting with {@code /} is sent as a command, e.g. {@code /msg Steve {msg}}.
 *
 * @param id       {@code all}, {@code global}, {@code local}, {@code friends}, {@code trade}, or
 *                 {@code custom:<n>}
 * @param name     the caption of a custom button; empty for built-ins, which are translated
 * @param template a custom button's template; empty for built-ins
 */
public record ChatButton(String id, String name, String template) {

    public static final String ALL = "all";
    public static final String CUSTOM_PREFIX = "custom:";

    public static final ChatButton ALL_BUTTON = new ChatButton(ALL, "", "");
    public static final ChatButton GLOBAL_BUTTON = new ChatButton(ChatChannel.GLOBAL.id(), "", "");
    public static final ChatButton LOCAL_BUTTON = new ChatButton(ChatChannel.LOCAL.id(), "", "");
    public static final ChatButton FRIENDS_BUTTON = new ChatButton(ChatChannel.FRIENDS.id(), "", "");
    public static final ChatButton TRADE_BUTTON = new ChatButton(ChatChannel.TRADE.id(), "", "");

    public boolean custom() {
        return id.startsWith(CUSTOM_PREFIX);
    }

    /** The channel this button shows, or null for All (everything) and for custom buttons. */
    @Nullable
    public ChatChannel channel() {
        for (ChatChannel channel : ChatChannel.values()) {
            if (channel.id().equals(id)) return channel;
        }
        return null;
    }

    public Component title() {
        if (custom()) return Component.literal(name);
        if (ALL.equals(id)) return Component.translatable("gui.roll_mod.chat.channel.all");
        ChatChannel channel = channel();
        return channel != null ? channel.title() : Component.literal(id);
    }

    /**
     * What to send for {@code text} through this button — always a command (leading {@code /}),
     * since the server locks plain chat: Global {@code /gc}, All and Local {@code /lc}, Friends
     * {@code /fc}, Trade {@code /tc}. A custom template that is not itself a command goes to local
     * chat through {@code /lc}.
     */
    public String outgoing(String text) {
        if (custom()) {
            String filled = template.contains("{msg}") ? template.replace("{msg}", text) : template + " " + text;
            return filled.startsWith("/") ? filled : "/lc " + filled;
        }
        return switch (id) {
            case "global" -> "/gc " + text;
            case "friends" -> "/fc " + text;
            case "trade" -> "/tc " + text;
            default -> "/lc " + text;
        };
    }
}
