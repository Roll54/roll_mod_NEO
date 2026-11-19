package com.roll_54.roll_mod.minestar.chat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The chat lines kept between sessions: the latest {@link #KEEP} lines the client received, per server
 * (or per singleplayer world), in {@code minestar/chat_history/<server>.json}. {@link ClientChatStore}
 * writes them as the player leaves (and every so often while playing, so a crash loses little) and
 * reads them back as the player joins the same server again.
 *
 * <p>Lines are stored as component JSON, so colours, links and hover text come back as they were. A line
 * that cannot be written or read back (some mod's own component contents) is skipped, not the file.
 */
@OnlyIn(Dist.CLIENT)
final class ClientChatSaves {

    /** How many of the latest lines are kept. */
    static final int KEEP = 128;

    /** One stored line. */
    record Saved(ChatChannel channel, Component message, String sender, int count, long time) {}

    private ClientChatSaves() {}

    /** The file for the server or world the client is on now; null when on neither. */
    @Nullable
    static Path file() {
        Minecraft mc = Minecraft.getInstance();
        String key;
        var integrated = mc.getSingleplayerServer();
        if (integrated != null) {
            Path root = integrated.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            Path folder = root.getFileName() == null || root.getFileName().toString().equals(".")
                    ? root.getParent() : root;
            key = "world_" + (folder == null || folder.getFileName() == null ? "unknown" : folder.getFileName());
        } else {
            ServerData server = mc.getCurrentServer();
            if (server == null || server.ip == null || server.ip.isBlank()) return null;
            key = "server_" + server.ip.trim().toLowerCase(Locale.ROOT);
        }
        String safe = key.replaceAll("[^a-zA-Z0-9._-]", "_");
        return MinestarFiles.resolve("chat_history", safe + ".json");
    }

    static void write(Path file, List<Saved> lines, HolderLookup.Provider registries) {
        JsonArray array = new JsonArray();
        for (Saved line : lines.subList(Math.max(0, lines.size() - KEEP), lines.size())) {
            try {
                JsonObject entry = new JsonObject();
                entry.addProperty("channel", line.channel().name());
                entry.addProperty("sender", line.sender());
                entry.addProperty("count", line.count());
                entry.addProperty("time", line.time());
                entry.add("message", JsonParser.parseString(Component.Serializer.toJson(line.message(), registries)));
                array.add(entry);
            } catch (Exception e) {
                RollMod.LOGGER.debug("[Chat] a line could not be saved: {}", line.message().getString(), e);
            }
        }
        JsonObject root = new JsonObject();
        root.add("lines", array);
        MinestarFiles.write(file, root);
    }

    static List<Saved> read(Path file, HolderLookup.Provider registries) {
        List<Saved> lines = new ArrayList<>();
        JsonObject root = MinestarFiles.readObject(file);
        if (!root.has("lines") || !root.get("lines").isJsonArray()) return lines;
        for (JsonElement element : root.getAsJsonArray("lines")) {
            try {
                JsonObject entry = element.getAsJsonObject();
                ChatChannel channel = ChatChannel.valueOf(entry.get("channel").getAsString());
                Component message = Component.Serializer.fromJson(entry.get("message"), registries);
                if (message == null) continue;
                String sender = entry.has("sender") ? entry.get("sender").getAsString() : "";
                int count = entry.has("count") ? Math.max(1, entry.get("count").getAsInt()) : 1;
                long time = entry.has("time") ? entry.get("time").getAsLong() : 0L;
                lines.add(new Saved(channel, message, sender, count, time));
            } catch (Exception e) {
                RollMod.LOGGER.debug("[Chat] a saved line in {} could not be read", file, e);
            }
        }
        return lines.size() > KEEP ? new ArrayList<>(lines.subList(lines.size() - KEEP, lines.size())) : lines;
    }
}
