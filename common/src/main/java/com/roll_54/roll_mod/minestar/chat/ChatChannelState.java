package com.roll_54.roll_mod.minestar.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The player's chat buttons: their order, which are hidden, the custom ones, and which one was
 * picked last. Client state, kept in {@code config/roll_mod/chat_channels.json} — same shape as
 * {@code UiThemeState}. Edited from the hub's Settings tab, read by the buttons over the chat box.
 *
 * <p>Built-in buttons can be hidden but never removed, and at least one button always stays
 * visible, so the chat box can never end up with no way to pick a channel.
 */
public final class ChatChannelState {

    private static final int VERSION = 1;
    /**
     * New built-ins go last and start visible: {@link #load} appends any built-in a saved file does
     * not list, after the player's own order, and nothing hides it until the player does.
     */
    private static final List<ChatButton> BUILT_INS = List.of(
            ChatButton.ALL_BUTTON, ChatButton.GLOBAL_BUTTON, ChatButton.LOCAL_BUTTON, ChatButton.FRIENDS_BUTTON,
            ChatButton.TRADE_BUTTON);

    private static ChatChannelState instance;

    /** Every button, hidden ones included, in display order. */
    private final List<ChatButton> order = new ArrayList<>(BUILT_INS);
    private final Set<String> hidden = new HashSet<>();
    /** The built-in button whose channel the chat shows; always All while a custom one sends. */
    private String view = ChatButton.ALL;
    /** The button messages go through: a built-in or a custom one. */
    private String send = ChatButton.ALL;
    private int nextCustom = 1;

    private ChatChannelState() {}

    public static synchronized ChatChannelState get() {
        if (instance == null) instance = load();
        return instance;
    }

    /* ------------------------------------------------ reading ------------------------------------------------ */

    public synchronized List<ChatButton> all() {
        return List.copyOf(order);
    }

    public synchronized List<ChatButton> visible() {
        List<ChatButton> out = new ArrayList<>();
        for (ChatButton button : order) {
            if (!hidden.contains(button.id())) out.add(button);
        }
        return out;
    }

    public synchronized boolean isHidden(String id) {
        return hidden.contains(id);
    }

    public synchronized ChatButton view() {
        return find(view, ChatButton.ALL_BUTTON);
    }

    public synchronized ChatButton send() {
        return find(send, view());
    }

    /* ------------------------------------------------ editing ------------------------------------------------ */

    /**
     * A click on a button over the chat box: a built-in one switches both the view and where
     * messages go; a custom one sends through itself and shows All, since it has no channel of its
     * own and its replies can come back on any.
     */
    public synchronized void pick(ChatButton button) {
        send = button.id();
        view = button.custom() ? ChatButton.ALL : button.id();
        save();
    }

    public synchronized void setHidden(String id, boolean hide) {
        if (hide) {
            // Never hide the last visible button.
            if (visible().size() <= 1 && !hidden.contains(id)) return;
            hidden.add(id);
        } else {
            hidden.remove(id);
        }
        save();
    }

    /** Moves a button one place earlier ({@code -1}) or later ({@code +1}). */
    public synchronized void move(String id, int delta) {
        int from = indexOf(id);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= order.size()) return;
        order.add(to, order.remove(from));
        save();
    }

    public synchronized void addCustom(String name, String template) {
        String cleanName = name.strip();
        String cleanTemplate = template.strip();
        if (cleanName.isEmpty() || cleanTemplate.isEmpty()) return;
        order.add(new ChatButton(ChatButton.CUSTOM_PREFIX + nextCustom++, cleanName, cleanTemplate));
        save();
    }

    public synchronized void removeCustom(String id) {
        if (!id.startsWith(ChatButton.CUSTOM_PREFIX)) return;
        order.removeIf(button -> button.id().equals(id));
        hidden.remove(id);
        if (send.equals(id)) send = view;
        save();
    }

    /* ---------------------------------------------- persistence ---------------------------------------------- */

    private void save() {
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        JsonArray orderJson = new JsonArray();
        JsonArray custom = new JsonArray();
        for (ChatButton button : order) {
            orderJson.add(button.id());
            if (button.custom()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("id", button.id());
                entry.addProperty("name", button.name());
                entry.addProperty("template", button.template());
                custom.add(entry);
            }
        }
        JsonArray hiddenJson = new JsonArray();
        hidden.forEach(hiddenJson::add);
        json.add("order", orderJson);
        json.add("hidden", hiddenJson);
        json.add("custom", custom);
        json.addProperty("view", view);
        json.addProperty("send", send);
        json.addProperty("nextCustom", nextCustom);

        Path path = file();
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            MinestarFiles.write(tmp, json);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Chat] could not save {}", path, e);
        }
    }

    /** A missing or broken file reads as the defaults; unknown ids are dropped, missing built-ins re-added. */
    private static ChatChannelState load() {
        ChatChannelState state = new ChatChannelState();
        JsonObject json = MinestarFiles.readObject(file());
        if (json.size() == 0) return state;
        try {
            Map<String, ChatButton> known = new LinkedHashMap<>();
            for (ChatButton button : BUILT_INS) known.put(button.id(), button);
            if (json.has("custom")) {
                for (JsonElement element : json.getAsJsonArray("custom")) {
                    JsonObject entry = element.getAsJsonObject();
                    String id = entry.get("id").getAsString();
                    if (!id.startsWith(ChatButton.CUSTOM_PREFIX)) continue;
                    known.put(id, new ChatButton(id, entry.get("name").getAsString(), entry.get("template").getAsString()));
                }
            }
            state.order.clear();
            if (json.has("order")) {
                for (JsonElement element : json.getAsJsonArray("order")) {
                    ChatButton button = known.remove(element.getAsString());
                    if (button != null) state.order.add(button);
                }
            }
            state.order.addAll(known.values());
            if (json.has("hidden")) {
                for (JsonElement element : json.getAsJsonArray("hidden")) state.hidden.add(element.getAsString());
            }
            if (state.visible().isEmpty()) state.hidden.clear();
            if (json.has("view")) state.view = json.get("view").getAsString();
            if (json.has("send")) state.send = json.get("send").getAsString();
            if (json.has("nextCustom")) state.nextCustom = Math.max(1, json.get("nextCustom").getAsInt());
        } catch (RuntimeException e) {
            RollMod.LOGGER.warn("[Chat] {} is malformed; using the default chat buttons", file(), e);
            return new ChatChannelState();
        }
        return state;
    }

    private ChatButton find(String id, ChatButton fallback) {
        int index = indexOf(id);
        return index < 0 ? fallback : order.get(index);
    }

    private int indexOf(String id) {
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).id().equals(id)) return i;
        }
        return -1;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("chat_channels.json");
    }
}
