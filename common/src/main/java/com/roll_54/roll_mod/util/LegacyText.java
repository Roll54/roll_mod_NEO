package com.roll_54.roll_mod.util;

import net.minecraft.network.chat.Component;

/**
 * Ampersand colour codes — {@code &1}, {@code &a}, {@code &l} and the rest — the way FTB and most
 * server plugins accept them, for the names players type themselves.
 *
 * <p>The typed text is what gets <em>stored</em>, codes and all. Converting on the way in would mean
 * the stored name no longer matches what the player typed, and every lookup — {@code /home <name>},
 * the uniqueness check, the search box — would have to know about the codes. So the rule is: {@link
 * #display} at every place a name is drawn, {@link #plain} at every place one is matched. A player
 * who called their home {@code &abase} still reaches it with {@code /home base}.
 *
 * <p>Section signs the client may have sent are dropped rather than honoured, so {@code &} is the
 * only way in and the two spellings cannot disagree.
 */
public final class LegacyText {

    /** The section sign the font renderer actually reads. */
    private static final char SECTION = '§';

    /**
     * Colours {@code 0-9 a-f}, styles {@code k} obfuscated, {@code l} bold, {@code m} strikethrough,
     * {@code n} underline, {@code o} italic, and {@code r} reset. Drop {@code k} from this string to
     * ban obfuscated names.
     */
    private static final String CODES = "0123456789abcdefklmnor";

    private LegacyText() {}

    /** The text as the font renderer wants it: {@code &a} becomes {@code §a}, {@code &&} a literal {@code &}. */
    public static String toSection(String raw) {
        return convert(raw, true);
    }

    /**
     * The text with every code removed — what searching, sorting and name lookups compare against,
     * so the codes are invisible to them rather than part of the name.
     */
    public static String plain(String raw) {
        return convert(raw, false);
    }

    /**
     * A name ready to draw. Safe to pass as a {@code Component.translatable} argument: the codes
     * apply within this component's own text and the style resets at its boundary, so colouring a
     * name does not bleed into the rest of the sentence.
     */
    public static Component display(String raw) {
        return Component.literal(toSection(raw));
    }

    /**
     * What a text editor draws while the codes are being typed: every {@code &x} stays visible,
     * with the real section code slipped in front of it, so {@code &chello} shows as a red
     * {@code &chello}.
     *
     * <p>Keeping the codes on screen is what keeps the editor usable: a section pair draws at zero
     * width, so every visible character sits exactly where the editor measured it from the raw
     * text and the cursor and selection still line up. Bold ({@code l}) and obfuscated ({@code k})
     * are left out for the same reason — bold widens every glyph after it and would drag the text
     * out from under the cursor. They still apply wherever the text is finally shown.
     */
    public static String editorPreview(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == SECTION) continue;
            if (c == '&' && i + 1 < raw.length()) {
                char next = Character.toLowerCase(raw.charAt(i + 1));
                if (next == '&') {
                    out.append("&&");
                    i++;
                    continue;
                }
                if (isCode(next) && next != 'l' && next != 'k') {
                    out.append(SECTION).append(next);
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String convert(String raw, boolean keep) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            // A section sign that arrived in the payload is not the player's to set.
            if (c == SECTION) continue;
            if (c == '&' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if (next == '&') {          // escaped: one literal ampersand
                    out.append('&');
                    i++;
                    continue;
                }
                if (isCode(next)) {
                    if (keep) out.append(SECTION).append(Character.toLowerCase(next));
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isCode(char c) {
        return CODES.indexOf(Character.toLowerCase(c)) >= 0;
    }
}
