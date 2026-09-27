package com.roll_54.roll_mod.minestar.moderation;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;

import java.nio.file.Path;

/**
 * The mod's own "server is closed" switch, and the message it closes with.
 *
 * <p>Not vanilla's whitelist, and not a wrapper over it. The two mean different things: vanilla's
 * is an allow-list of names, this is a single toggle whose allow-list is a permission — operators
 * and whoever holds the moderation node get in, everybody else is turned away. Expressing that as
 * entries in {@code whitelist.json} would mean rewriting the file every time someone's rank
 * changed.
 *
 * <p>{@code kickMessage} is stored with {@code &} colour codes, the mod's one markup convention,
 * and rendered through {@code LegacyText} at the point of use. {@code \n} in the string becomes a
 * line break on the disconnect screen.
 */
public final class WhitelistStore {

    private static final String FILE = "whitelist.json";

    private static final String DEFAULT_MESSAGE =
            "&cThe server is closed right now.\n&7Try again shortly.";

    private static volatile boolean enabled;
    private static volatile String kickMessage = DEFAULT_MESSAGE;
    private static volatile boolean loaded;

    private WhitelistStore() {}

    /** True while everybody without a bypass is being turned away. */
    public static boolean enabled() {
        load();
        return enabled;
    }

    /** The raw message, still carrying its {@code &} codes. */
    public static String kickMessage() {
        load();
        return kickMessage;
    }

    public static synchronized void setEnabled(boolean value) {
        load();
        if (enabled == value) return;
        enabled = value;
        save();
    }

    public static synchronized void setKickMessage(String value) {
        load();
        String message = value == null || value.isBlank() ? DEFAULT_MESSAGE : value;
        if (message.equals(kickMessage)) return;
        kickMessage = message;
        save();
    }

    public static void reload() {
        ModerationViewers.markDirty();
        synchronized (WhitelistStore.class) {
            enabled = false;
            kickMessage = DEFAULT_MESSAGE;
            loaded = false;
        }
    }

    private static void load() {
        if (loaded) return;
        synchronized (WhitelistStore.class) {
            if (loaded) return;
            JsonObject root = MinestarFiles.readObject(path());
            enabled = root.has("enabled") && root.get("enabled").getAsBoolean();
            kickMessage = root.has("kickMessage")
                    ? root.get("kickMessage").getAsString()
                    : DEFAULT_MESSAGE;
            loaded = true;
        }
    }

    private static void save() {
        ModerationViewers.markDirty();
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        root.addProperty("kickMessage", kickMessage);
        MinestarFiles.write(path(), root);
    }

    private static Path path() {
        return MinestarFiles.resolve(FILE);
    }
}
