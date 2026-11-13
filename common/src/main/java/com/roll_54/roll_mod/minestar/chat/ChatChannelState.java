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
     * The order a fresh config starts with. A saved file keeps its own order: {@link #load} appends
     * any built-in it does not list after the player's buttons, visible until the player hides it.
     */
    private static final List<ChatButton> BUILT_INS = List.of(
            ChatButton.ALL_BUTTON, ChatButton.LOCAL_BUTTON, ChatButton.GLOBAL_BUTTON, ChatButton.TEAM_BUTTON, ChatButton.PM_BUTTON,
            ChatButton.SPAM_BUTTON, ChatButton.TRADE_BUTTON, ChatButton.DEV_BUTTON);

    private static ChatChannelState instance;

    /** Every button, hidden ones included, in display order. */
    private final List<ChatButton> order = new ArrayList<>(BUILT_INS);
    /**
     * Unticked buttons: not over the chat box. Spam and DEV start unticked — noise next to people
     * talking — until the player ticks them in Settings.
     */
    private final Set<String> hidden = new HashSet<>(Set.of(ChatChannel.SPAM.id(), ChatChannel.DEV.id()));
    /**
     * Channels the All tab leaves out, set apart from {@link #hidden}: a channel can have no button over the
     * chat box and still show in All, or the other way round. Spam and DEV start out of All.
     */
    private final Set<String> notInAll = new HashSet<>(Set.of(ChatChannel.SPAM.id(), ChatChannel.DEV.id()));
    /** The built-in button whose channel the chat shows; always All while a custom one sends. */
    private String view = ChatButton.ALL;
    /** The button messages go through: a built-in or a custom one. */
    private String send = ChatButton.ALL;
    private int nextCustom = 1;
    /** Whether a message typed with a leading {@code !} goes to Global, whatever button is picked. */
    private boolean bangGlobal;
    /** Whether "@Name" mentions of the player ping ({@code MentionSound}). Superseded by {@link #mentionVolume}. */
    private boolean mentionSound = true;
    /** How loud the mention ping is, 0-100; 0 is off. */
    private int mentionVolume = 100;
    /** Whose deaths and whose advancements show in chat. */
    private MessagesFrom deaths = MessagesFrom.EVERYONE;
    private MessagesFrom advancements = MessagesFrom.EVERYONE;
    /** Lines from the Discord bridge, and Wixie's tips (the server's timed broadcasts), left out of the chat. */
    private boolean hideDiscord;
    private boolean hideWixie;
    /** Per channel id: what a new line there does. Missing means {@link Notify#MENTIONS}. */
    private final Map<String, Notify> notify = new LinkedHashMap<>();

    /** Whose deaths or advancements are shown: everyone's, the player's own team's (FTB Teams), or nobody's. */
    public enum MessagesFrom {
        EVERYONE, TEAM, NOBODY;

        public net.minecraft.network.chat.Component title() {
            return net.minecraft.network.chat.Component.translatable(
                    "gui.roll_mod.hub.settings.messages_from." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    /** What a channel's new lines do: a soft sound for each one plus the ping, the ping only, or nothing. */
    public enum Notify {
        ALL, MENTIONS, NOTHING;

        public net.minecraft.network.chat.Component title() {
            return net.minecraft.network.chat.Component.translatable(
                    "gui.roll_mod.hub.settings.notify." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

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

    /** Whether the All tab shows lines of {@code channel}, as set in Settings ("In All"). */
    public synchronized boolean inAll(ChatChannel channel) {
        return !notInAll.contains(channel.id());
    }

    public synchronized boolean isInAll(String id) {
        return !notInAll.contains(id);
    }

    public synchronized boolean bangGlobal() {
        return bangGlobal;
    }

    /* ------------------------------------------------ editing ------------------------------------------------ */

    public synchronized void setBangGlobal(boolean on) {
        bangGlobal = on;
        save();
    }

    public synchronized boolean mentionSound() {
        return mentionSound && mentionVolume > 0;
    }

    public synchronized void setMentionSound(boolean on) {
        mentionSound = on;
        save();
    }

    public synchronized int mentionVolume() {
        return mentionSound ? mentionVolume : 0;
    }

    public synchronized void setMentionVolume(int volume) {
        mentionVolume = Math.max(0, Math.min(100, volume));
        mentionSound = true;
        save();
    }

    public synchronized MessagesFrom deaths() {
        return deaths;
    }

    public synchronized void setDeaths(MessagesFrom from) {
        deaths = from;
        save();
    }

    public synchronized MessagesFrom advancements() {
        return advancements;
    }

    public synchronized void setAdvancements(MessagesFrom from) {
        advancements = from;
        save();
    }

    public synchronized boolean hideDiscord() {
        return hideDiscord;
    }

    public synchronized void setHideDiscord(boolean hide) {
        hideDiscord = hide;
        save();
    }

    public synchronized boolean hideWixie() {
        return hideWixie;
    }

    public synchronized void setHideWixie(boolean hide) {
        hideWixie = hide;
        save();
    }

    public synchronized Notify notify(ChatChannel channel) {
        return notify.getOrDefault(channel.id(), Notify.MENTIONS);
    }

    public synchronized void setNotify(ChatChannel channel, Notify level) {
        notify.put(channel.id(), level);
        save();
    }

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

    public synchronized void setInAll(String id, boolean in) {
        if (in) notInAll.remove(id);
        else notInAll.add(id);
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
        JsonArray notInAllJson = new JsonArray();
        notInAll.forEach(notInAllJson::add);
        json.add("notInAll", notInAllJson);
        json.add("custom", custom);
        json.addProperty("view", view);
        json.addProperty("send", send);
        json.addProperty("nextCustom", nextCustom);
        json.addProperty("bangGlobal", bangGlobal);
        json.addProperty("mentionSound", mentionSound);
        json.addProperty("mentionVolume", mentionVolume);
        json.addProperty("deaths", deaths.name());
        json.addProperty("advancements", advancements.name());
        json.addProperty("hideDiscord", hideDiscord);
        json.addProperty("hideWixie", hideWixie);
        JsonObject notifyJson = new JsonObject();
        notify.forEach((id, level) -> notifyJson.addProperty(id, level.name()));
        json.add("notify", notifyJson);

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
                state.hidden.clear();
                for (JsonElement element : json.getAsJsonArray("hidden")) state.hidden.add(element.getAsString());
            }
            if (state.visible().isEmpty()) state.hidden.clear();
            if (json.has("view")) state.view = json.get("view").getAsString();
            if (json.has("send")) state.send = json.get("send").getAsString();
            if (json.has("nextCustom")) state.nextCustom = Math.max(1, json.get("nextCustom").getAsInt());
            if (json.has("bangGlobal")) state.bangGlobal = json.get("bangGlobal").getAsBoolean();
            if (json.has("mentionSound")) state.mentionSound = json.get("mentionSound").getAsBoolean();
            if (json.has("mentionVolume")) state.mentionVolume = Math.max(0, Math.min(100, json.get("mentionVolume").getAsInt()));
            if (json.has("deaths")) state.deaths = enumOr(MessagesFrom.class, json.get("deaths").getAsString(), MessagesFrom.EVERYONE);
            if (json.has("advancements")) state.advancements = enumOr(MessagesFrom.class, json.get("advancements").getAsString(), MessagesFrom.EVERYONE);
            if (json.has("hideDiscord")) state.hideDiscord = json.get("hideDiscord").getAsBoolean();
            if (json.has("hideWixie")) state.hideWixie = json.get("hideWixie").getAsBoolean();
            if (json.has("notify")) {
                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("notify").entrySet()) {
                    state.notify.put(entry.getKey(), enumOr(Notify.class, entry.getValue().getAsString(), Notify.MENTIONS));
                }
            }
            state.notInAll.clear();
            if (json.has("notInAll")) {
                for (JsonElement element : json.getAsJsonArray("notInAll")) state.notInAll.add(element.getAsString());
            } else {
                // Files from before the separate setting: All left out the unticked buttons, so it still does.
                state.notInAll.addAll(state.hidden);
            }
        } catch (RuntimeException e) {
            RollMod.LOGGER.warn("[Chat] {} is malformed; using the default chat buttons", file(), e);
            return new ChatChannelState();
        }
        return state;
    }

    private static <E extends Enum<E>> E enumOr(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
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
