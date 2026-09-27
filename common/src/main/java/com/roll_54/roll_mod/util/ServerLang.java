package com.roll_54.roll_mod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roll_54.roll_mod.RollMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;

/**
 * Ukrainian text resolved on the server, for text that is <em>stored</em> rather than shown — a
 * letter's body is kept as a plain string and read later by whoever opens it, so it cannot be a
 * {@code Component.translatable} the reader's client resolves.
 *
 * <p>A dedicated server only ever loads {@code en_us}, so this reads {@code uk_ua.json} straight out
 * of the mod jar, once, on first use. The keys are the ordinary lang keys, which keeps the wording
 * in the lang files with everything else.
 */
public final class ServerLang {

    private static final String FILE = "/assets/" + RollMod.MODID + "/lang/uk_ua.json";

    private static Map<String, String> uk;

    private ServerLang() {}

    /** The Ukrainian text for {@code key}, formatted like a lang string; the key itself if missing. */
    public static String uk(String key, Object... args) {
        String pattern = table().getOrDefault(key, key);
        if (args.length == 0) return pattern;
        try {
            return String.format(pattern, args);
        } catch (IllegalFormatException e) {
            return pattern;
        }
    }

    private static synchronized Map<String, String> table() {
        if (uk != null) return uk;
        Map<String, String> map = new HashMap<>();
        try (InputStream in = ServerLang.class.getResourceAsStream(FILE)) {
            if (in == null) {
                RollMod.LOGGER.warn("[ServerLang] {} is missing from the jar; keys will show as-is.", FILE);
            } else {
                JsonObject json = JsonParser.parseReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : json.entrySet()) {
                    if (e.getValue().isJsonPrimitive()) map.put(e.getKey(), e.getValue().getAsString());
                }
            }
        } catch (Exception e) {
            RollMod.LOGGER.error("[ServerLang] Could not read {}.", FILE, e);
        }
        uk = map;
        return uk;
    }
}
