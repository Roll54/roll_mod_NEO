package com.roll_54.roll_mod.minestar.chat.emoji;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roll_54.roll_mod.RollMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Ported from Eden_LD (MIT, (c) 2026 LeDok; see docs/third-party-licenses.txt).
 *
 * <p>Keeps emoji.json in step with the Discord server's own emojis, read through a bot the owner adds
 * to their server (the bot needs no permissions, only membership). New emojis are appended under
 * their Discord name, deleted ones leave the list, and the in-game names chosen in emoji.json are
 * kept (matched by id). Animated emojis are marked as such.
 *
 * <p>The token lives in {@code config/roll_mod/discord.json}, a file of its own that only the server
 * reads — never in the main config, which is synced to every client.
 */
final class DiscordEmojiSync {

    private static final String API = "https://discord.com/api/v10/guilds/%s/emojis";
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("discord.json");

    private record Remote(String name, boolean animated) {}

    private record Settings(String token, String guild, int syncMinutes) {}

    private static Settings settings;

    private DiscordEmojiSync() {}

    static boolean enabled() {
        Settings current = settings();
        return !current.token().isBlank() && !current.guild().isBlank();
    }

    static int syncMinutes() {
        return settings().syncMinutes();
    }

    private static synchronized Settings settings() {
        if (settings == null) reread();
        return settings;
    }

    /** discord.json, written with empty values when missing. Re-read on every {@code /emojis reload}. */
    static synchronized void reread() {
        try {
            if (!Files.exists(FILE)) {
                JsonObject root = new JsonObject();
                root.addProperty("_comment", "Discord emoji sync (optional). discord_bot_token: a bot of your own "
                        + "(Discord Developer Portal > New Application > Bot > Reset Token), invited to your server "
                        + "with scope 'bot' and no permissions. Keep it secret. discord_guild_id: right-click your "
                        + "server > Copy Server ID. emoji_sync_minutes: how often the list follows Discord (0 = only "
                        + "on start and /emojis reload).");
                root.addProperty("discord_bot_token", "");
                root.addProperty("discord_guild_id", "");
                root.addProperty("emoji_sync_minutes", 60);
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            }
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            settings = new Settings(string(root, "discord_bot_token"), string(root, "discord_guild_id"),
                    root.has("emoji_sync_minutes") ? Math.max(0, root.get("emoji_sync_minutes").getAsInt()) : 60);
        } catch (IOException | RuntimeException e) {
            RollMod.LOGGER.error("[Emoji] Can't read {}; the Discord sync is off.", FILE, e);
            settings = new Settings("", "", 0);
        }
    }

    private static String string(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonPrimitive() ? root.get(key).getAsString().trim() : "";
    }

    /** The emoji list merged with the server's current emojis; {@code list} itself when Discord can't be reached. */
    static JsonObject merge(JsonObject list) {
        Map<String, Remote> remote;
        try {
            remote = fetch();
        } catch (IOException e) {
            RollMod.LOGGER.warn("[Emoji] Discord sync failed: {}", e.getMessage());
            return list;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return list;
        }
        JsonObject merged = new JsonObject();
        Set<String> kept = new HashSet<>();
        int removed = 0;
        // Known emojis keep their place and their in-game name.
        for (Map.Entry<String, JsonElement> entry : list.entrySet()) {
            String id = entry.getValue().isJsonPrimitive() ? EmojiRegistry.idOf(entry.getValue().getAsString().trim()) : null;
            Remote emoji = id == null ? null : remote.get(id);
            if (emoji == null) {
                removed++;
                continue;
            }
            merged.addProperty(entry.getKey(), value(id, emoji));
            kept.add(id);
        }
        int added = 0;
        for (Map.Entry<String, Remote> entry : remote.entrySet()) {
            if (kept.contains(entry.getKey())) continue;
            merged.addProperty(freeName(merged, entry.getValue().name()), value(entry.getKey(), entry.getValue()));
            added++;
        }
        if (added > 0 || removed > 0) RollMod.LOGGER.info("[Emoji] Discord sync: {} added, {} removed.", added, removed);
        return merged;
    }

    /** The server's usable emojis by id (unavailable ones — lost boost slots — are left out). */
    private static Map<String, Remote> fetch() throws IOException, InterruptedException {
        Settings current = settings();
        HttpResponse<String> response = EmojiRegistry.HTTP.send(HttpRequest.newBuilder(URI.create(String.format(API, current.guild())))
                        .header("Authorization", "Bot " + current.token())
                        .header("User-Agent", "DiscordBot (https://github.com/Roll54, 1) roll_mod")
                        .timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Discord answered " + response.statusCode()
                    + (response.statusCode() == 401 ? " (check discord_bot_token)"
                    : response.statusCode() == 403 || response.statusCode() == 404
                    ? " (is the bot in the server? check discord_guild_id)" : ""));
        }
        Map<String, Remote> emojis = new LinkedHashMap<>();
        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
        for (JsonElement element : array) {
            JsonObject emoji = element.getAsJsonObject();
            if (emoji.has("available") && !emoji.get("available").getAsBoolean()) continue;
            emojis.put(emoji.get("id").getAsString(), new Remote(emoji.get("name").getAsString(),
                    emoji.has("animated") && emoji.get("animated").getAsBoolean()));
        }
        return emojis;
    }

    private static String value(String id, Remote emoji) {
        return emoji.animated() ? "a:" + id : id;
    }

    /** A typeable in-game name, unique in the list ("name", "name_2", …). */
    private static String freeName(JsonObject list, String discordName) {
        String base = discordName.replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (base.length() > 28) base = base.substring(0, 28);
        if (base.isEmpty()) base = "emoji";
        String name = base;
        for (int i = 2; hasName(list, name); i++) name = base + "_" + i;
        return name;
    }

    private static boolean hasName(JsonObject list, String name) {
        for (String key : list.keySet()) {
            if (key.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
