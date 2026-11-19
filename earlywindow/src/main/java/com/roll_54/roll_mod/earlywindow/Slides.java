package com.roll_54.roll_mod.earlywindow;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player bases shown behind the loading screen, one per JSON file in
 * {@code common/src/main/resources/assets/roll_mod/loading/} (built into the earlywindow jar only):
 * <pre>
 * {
 *   "image": "spawn.png",            // next to the JSON; PNG or JPG
 *   "date": "Season 3",              // which ModernTech season it is from
 *   "description": "Spawn Megabase", // the building's name
 *   "authors": "Roll54, Lorp_ooo"    // who built it
 * }
 * </pre>
 * Keys are matched case-insensitively. The early window's font has Latin letters only.
 */
final class Slides {

    static final String DIR = "/roll_mod-loading/";
    private static final Logger LOGGER = LoggerFactory.getLogger("roll_mod-earlywindow");

    record Slide(String image, String date, String description, String authors) {}

    /** A decoded image, RGBA, ready for upload; {@link #free} once uploaded. */
    record Pixels(ByteBuffer data, int width, int height) {
        void free() {
            STBImage.stbi_image_free(data);
        }
    }

    private Slides() {}

    /** Every slide in the jar, shuffled — a new order each launch. */
    static List<Slide> loadShuffled() {
        List<Slide> slides = new ArrayList<>();
        String index = readText("index.txt");
        if (index == null) return slides;
        for (String line : index.split("\n")) {
            String name = line.trim();
            if (name.isEmpty()) continue;
            String json = readText(name);
            if (json == null) continue;
            try {
                Map<String, String> fields = parseFlatObject(json);
                String image = fields.get("image");
                if (image == null || image.isBlank()) {
                    LOGGER.warn("Loading slide {} has no \"image\"; skipped", name);
                    continue;
                }
                slides.add(new Slide(image.trim(), fields.getOrDefault("date", ""),
                        fields.getOrDefault("description", ""), fields.getOrDefault("authors", "")));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Loading slide {} is not valid JSON: {}", name, e.getMessage());
            }
        }
        Collections.shuffle(slides);
        return slides;
    }

    /** Decodes an image from the jar, or null if it is missing or not a PNG/JPG stb can read. */
    @Nullable
    static Pixels decode(String file) {
        byte[] bytes = readBytes(file);
        if (bytes == null) {
            LOGGER.warn("Loading image {} is not in the jar", file);
            return null;
        }
        ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
        try {
            encoded.put(bytes).flip();
            int[] w = new int[1], h = new int[1], channels = new int[1];
            ByteBuffer data = STBImage.stbi_load_from_memory(encoded, w, h, channels, 4);
            if (data == null) {
                LOGGER.warn("Loading image {} could not be decoded: {}", file, STBImage.stbi_failure_reason());
                return null;
            }
            return new Pixels(data, w[0], h[0]);
        } finally {
            MemoryUtil.memFree(encoded);
        }
    }

    @Nullable
    private static String readText(String file) {
        byte[] bytes = readBytes(file);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    @Nullable
    private static byte[] readBytes(String file) {
        try (InputStream in = Slides.class.getResourceAsStream(DIR + file)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            LOGGER.warn("Could not read {}", file, e);
            return null;
        }
    }

    /**
     * A JSON object of string values — all a slide is. No JSON library is reachable this early, and
     * this one shape does not need one. Non-string values are kept as their raw text.
     */
    static Map<String, String> parseFlatObject(String json) {
        Map<String, String> out = new HashMap<>();
        int[] i = {0};
        skipSpace(json, i);
        expect(json, i, '{');
        skipSpace(json, i);
        if (peek(json, i) == '}') return out;
        while (true) {
            skipSpace(json, i);
            String key = readString(json, i);
            skipSpace(json, i);
            expect(json, i, ':');
            skipSpace(json, i);
            String value;
            if (peek(json, i) == '"') {
                value = readString(json, i);
            } else {
                int start = i[0];
                while (i[0] < json.length() && ",}".indexOf(json.charAt(i[0])) < 0) i[0]++;
                value = json.substring(start, i[0]).trim();
            }
            out.put(key.toLowerCase(java.util.Locale.ROOT), value);
            skipSpace(json, i);
            char c = next(json, i);
            if (c == '}') return out;
            if (c != ',') throw new IllegalArgumentException("expected ',' or '}' at " + (i[0] - 1));
        }
    }

    private static String readString(String s, int[] i) {
        expect(s, i, '"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next(s, i);
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = next(s, i);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    if (i[0] + 4 > s.length()) throw new IllegalArgumentException("bad \\u escape");
                    sb.append((char) Integer.parseInt(s.substring(i[0], i[0] + 4), 16));
                    i[0] += 4;
                }
                default -> sb.append(e);
            }
        }
    }

    private static void skipSpace(String s, int[] i) {
        while (i[0] < s.length() && Character.isWhitespace(s.charAt(i[0]))) i[0]++;
    }

    private static char peek(String s, int[] i) {
        if (i[0] >= s.length()) throw new IllegalArgumentException("unexpected end");
        return s.charAt(i[0]);
    }

    private static char next(String s, int[] i) {
        char c = peek(s, i);
        i[0]++;
        return c;
    }

    private static void expect(String s, int[] i, char c) {
        if (next(s, i) != c) throw new IllegalArgumentException("expected '" + c + "' at " + (i[0] - 1));
    }
}
