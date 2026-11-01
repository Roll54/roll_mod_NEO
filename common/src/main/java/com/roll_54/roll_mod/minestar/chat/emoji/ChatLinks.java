package com.roll_54.roll_mod.minestar.chat.emoji;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.util.Locale;

/**
 * Links in chat lines, clickable: a GIF (Tenor, Giphy, Klipy or any {@code .gif}, which is what
 * Discord's GIF picker posts) as a short {@code [GIF]}, anything else as the link itself. The link
 * shows on hover. A click on a link asks before opening it in the browser, as vanilla links do; a
 * click on {@code [GIF]} plays it in the chat instead ({@code GifViewer}).
 */
public final class ChatLinks {

    /** Trailing characters that end a sentence, not the link. */
    private static final String TRAILING = ".,;:!?)]}'\"";

    private ChatLinks() {}

    static MutableComponent link(String url) {
        String trimmed = trimPunctuation(url);
        boolean gif = isGif(trimmed);
        MutableComponent link = Component.literal(gif ? "[GIF]" : trimmed).withStyle(style -> style
                .withColor(gif ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA)
                .withUnderlined(!gif)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, trimmed))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(trimmed))));
        String rest = url.substring(trimmed.length());
        return rest.isEmpty() ? link : Component.empty().append(link).append(rest);
    }

    public static boolean isGif(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            return host.endsWith("tenor.com") || host.endsWith("giphy.com") || host.endsWith("klipy.com")
                    || path.endsWith(".gif");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String trimPunctuation(String url) {
        int end = url.length();
        while (end > 0 && TRAILING.indexOf(url.charAt(end - 1)) >= 0) end--;
        return url.substring(0, end);
    }
}
