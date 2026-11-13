package com.roll_54.roll_mod.minestar.hub.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Settings → Notifications, ported from Eden_LD (without its guild, party, activity and PvP kinds): what the hub's
 * tabs point out ({@link Kind}), and how — a badge, a glow, or both ({@link Style}) — in which colour ({@link Color}).
 * Client preference, in {@code config/roll_mod/hub_notifications.json}; read every frame by the tab strip, so the
 * reads are plain field reads. Off the client (the server's copy of the hub tree) everything reads as on.
 */
public final class HubNotifications {

    /** What a tab can point out. */
    public enum Kind {
        /** A dot on each tab until it is opened once. */
        NEW_TABS,
        FRIEND_REQUESTS,
        TELEPORTS,
        MONEY,
        DAILIES,
        AUCTION,
        HOME_INVITES,
        MAIL,
        MODERATION;

        public Component title() {
            return Component.translatable("gui.roll_mod.hub.settings.notify_kind." + name().toLowerCase(Locale.ROOT));
        }

        public Component hint() {
            return Component.translatable("gui.roll_mod.hub.settings.notify_kind." + name().toLowerCase(Locale.ROOT) + ".tip");
        }
    }

    /** How a tab with news shows it. */
    public enum Style {
        BADGE, GLOW, BOTH;

        public Component title() {
            return Component.translatable("gui.roll_mod.hub.settings.notify_style." + name().toLowerCase(Locale.ROOT));
        }

        public boolean badge() {
            return this != GLOW;
        }

        public boolean glow() {
            return this != BADGE;
        }
    }

    /** The colour of the badge and the glow; {@link #THEME} is the hub theme's own accent. */
    public enum Color {
        THEME(0), RED(0xFFE5484D), ORANGE(0xFFF0A04B), YELLOW(0xFFE8C517), GREEN(0xFF4ADE80), CYAN(0xFF22D3EE),
        BLUE(0xFF3B82F6), PURPLE(0xFFA855F7), PINK(0xFFEC4899);

        private final int argb;

        Color(int argb) {
            this.argb = argb;
        }

        public Component title() {
            return Component.translatable("gui.roll_mod.hub.settings.notify_color." + name().toLowerCase(Locale.ROOT));
        }

        /** ARGB; for {@link #THEME} the theme's accent, or the old amber dot on themes without a palette. */
        public int argb() {
            if (this != THEME) return argb;
            UiTheme.Palette palette = FMLEnvironment.dist == Dist.CLIENT ? UiThemeState.get().palette() : null;
            return palette != null ? palette.accent() : HubBadge.DOT_FILL;
        }
    }

    private static final Set<Kind> OFF = EnumSet.noneOf(Kind.class);
    /** Tab ids opened at least once, for {@link Kind#NEW_TABS}. */
    private static final Set<String> SEEN = new HashSet<>();
    private static Style style = Style.BADGE;
    private static Color color = Color.THEME;
    private static boolean loaded;

    private HubNotifications() {}

    public static synchronized boolean enabled(Kind kind) {
        if (FMLEnvironment.dist != Dist.CLIENT) return true;
        load();
        return !OFF.contains(kind);
    }

    public static synchronized void setEnabled(Kind kind, boolean on) {
        load();
        if (on) OFF.remove(kind);
        else OFF.add(kind);
        save();
    }

    /** Always the badge now: the style setting is gone (its glow is kept, unused). */
    public static synchronized Style style() {
        return Style.BADGE;
    }

    public static synchronized void setStyle(Style picked) {
        load();
        style = picked;
        save();
    }

    public static synchronized Color color() {
        load();
        return color;
    }

    public static synchronized void setColor(Color picked) {
        load();
        color = picked;
        save();
    }

    /** Whether the tab {@code id} wears the "new" dot: never opened, and the kind is on. */
    public static synchronized boolean unseen(String id) {
        if (FMLEnvironment.dist != Dist.CLIENT) return false;
        load();
        return !OFF.contains(Kind.NEW_TABS) && !SEEN.contains(id);
    }

    /** The tab {@code id} is on screen: it is not new any more. */
    public static synchronized void seen(String id) {
        load();
        if (SEEN.add(id)) save();
    }

    /* ---------------------------------------------- persistence ---------------------------------------------- */

    private static void load() {
        if (loaded) return;
        loaded = true;
        JsonObject json = MinestarFiles.readObject(file());
        if (json.size() == 0) return;
        try {
            if (json.has("off")) {
                for (JsonElement element : json.getAsJsonArray("off")) {
                    try {
                        OFF.add(Kind.valueOf(element.getAsString()));
                    } catch (IllegalArgumentException ignored) {
                        // A kind from another version.
                    }
                }
            }
            if (json.has("seen")) json.getAsJsonArray("seen").forEach(e -> SEEN.add(e.getAsString()));
            if (json.has("style")) style = Style.valueOf(json.get("style").getAsString());
            if (json.has("color")) color = Color.valueOf(json.get("color").getAsString());
        } catch (RuntimeException e) {
            RollMod.LOGGER.warn("[Hub] {} is malformed; using the default notifications", file(), e);
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        JsonArray off = new JsonArray();
        OFF.forEach(kind -> off.add(kind.name()));
        JsonArray seen = new JsonArray();
        SEEN.forEach(seen::add);
        json.add("off", off);
        json.add("seen", seen);
        json.addProperty("style", style.name());
        json.addProperty("color", color.name());
        Path path = file();
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            MinestarFiles.write(tmp, json);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Hub] could not save {}", path, e);
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("hub_notifications.json");
    }
}
