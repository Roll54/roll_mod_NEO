package com.roll_54.roll_mod.minestar.letters;

import com.roll_54.roll_mod.util.ItemMentions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A letter body line as the reader sees it: {@code &} colour codes, the same as
 * {@code LegacyText.display}, plus links.
 *
 * <ul>
 *   <li>{@code [shown text](https://example.com)} — the text is the link; it may carry its own
 *       {@code &} codes.</li>
 *   <li>a bare {@code https://example.com} — the address is the link. Trailing punctuation that
 *       ends a sentence ({@code .,;:!?} and closing brackets or quotes) is left out of it.</li>
 *   <li>{@code <item:minecraft:diamond>} — the item, as {@link ItemMentions} draws it. Only an
 *       operator can write one into a letter; see {@code LetterService}.</li>
 * </ul>
 *
 * <p>Only {@code http} and {@code https} become links; anything else stays plain text, so a letter
 * cannot hand the reader's OS a {@code file:} URI.
 *
 * <p>Built as styled runs rather than section-coded text, because a section code does not carry
 * from one component into the next: the colour before a link has to be re-applied after it, and the
 * click event has to sit on a component of its own.
 */
public final class LetterText {

    /** {@code [label](url)}, the label on one line and not empty. */
    private static final Pattern MARKDOWN = Pattern.compile("\\[([^\\]\\n]+)]\\((https?://[^\\s)]+)\\)");
    private static final Pattern BARE = Pattern.compile("https?://[^\\s]+");
    private static final String TRAILING = ".,;:!?)]}'\"";

    private static final String CODES = "0123456789abcdefklmnor";
    private static final char SECTION = '§';

    private LetterText() {}

    /** One line of a letter body, links clickable. */
    public static Component display(String raw) {
        MutableComponent out = Component.empty();
        if (raw == null || raw.isEmpty()) return out;
        append(out, raw, Style.EMPTY, true);
        return out;
    }

    /**
     * Walks {@code raw}, appending one component per run of equally styled text. {@code links} is
     * off inside a link's own label, where a nested link would mean nothing.
     *
     * @return the style in force at the end, which is what the text after a link label goes on with
     */
    private static Style append(MutableComponent out, String raw, Style start, boolean links) {
        Style style = start;
        StringBuilder run = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == SECTION) {          // not the writer's to set, as in LegacyText
                i++;
                continue;
            }
            if (c == '&' && i + 1 < raw.length()) {
                char next = Character.toLowerCase(raw.charAt(i + 1));
                if (next == '&') {
                    run.append('&');
                    i += 2;
                    continue;
                }
                if (CODES.indexOf(next) >= 0) {
                    flush(out, run, style);
                    style = apply(style, next);
                    i += 2;
                    continue;
                }
            }
            if (links && c == '[') {
                Matcher m = MARKDOWN.matcher(raw).region(i, raw.length());
                if (m.lookingAt()) {
                    flush(out, run, style);
                    // The label's own codes stay inside the label; the text after it goes on in
                    // the style it had before.
                    append(out, m.group(1), link(style, m.group(2)), false);
                    i = m.end();
                    continue;
                }
            }
            if (links && c == '<') {
                // Trusted as written: LetterService only lets an operator put a tag in a body.
                Matcher m = ItemMentions.TAG.matcher(raw).region(i, raw.length());
                MutableComponent mention = m.lookingAt() ? ItemMentions.mention(m.group(1), true) : null;
                if (mention != null) {
                    flush(out, run, style);
                    out.append(mention);
                    i = m.end();
                    continue;
                }
            }
            if (links && (c == 'h' || c == 'H') && (i == 0 || !Character.isLetterOrDigit(raw.charAt(i - 1)))) {
                Matcher m = BARE.matcher(raw).region(i, raw.length());
                if (m.lookingAt()) {
                    String url = m.group();
                    int end = url.length();
                    while (end > 0 && TRAILING.indexOf(url.charAt(end - 1)) >= 0) end--;
                    url = url.substring(0, end);
                    // "https://" on its own, or followed only by punctuation, is not an address.
                    if (url.indexOf("://") + 3 < url.length()) {
                        flush(out, run, style);
                        out.append(Component.literal(url).withStyle(link(style, url)));
                        i += end;
                        continue;
                    }
                }
            }
            run.append(c);
            i++;
        }
        flush(out, run, style);
        return style;
    }

    /** What a link looks like: aqua and underlined unless its label says otherwise, the address on hover. */
    private static Style link(Style base, String url) {
        return base.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal(url).withStyle(ChatFormatting.GRAY)));
    }

    /**
     * One {@code &x} code, the way the font renderer reads a section code: a colour, or {@code r},
     * clears every format before it; a format adds to what is there. Click and hover survive both,
     * so codes inside a link label do not unlink it.
     */
    private static Style apply(Style style, char code) {
        ChatFormatting format = ChatFormatting.getByCode(code);
        if (format == null) return style;
        if (format == ChatFormatting.RESET || format.isColor()) {
            Style reset = Style.EMPTY.withClickEvent(style.getClickEvent()).withHoverEvent(style.getHoverEvent());
            if (style.getClickEvent() != null) reset = reset.withUnderlined(true);
            return format.isColor() ? reset.withColor(format)
                    : style.getClickEvent() != null ? reset.withColor(ChatFormatting.AQUA) : reset;
        }
        return style.applyFormat(format);
    }

    private static void flush(MutableComponent out, StringBuilder run, Style style) {
        if (run.isEmpty()) return;
        out.append(Component.literal(run.toString()).withStyle(style));
        run.setLength(0);
    }
}
