package com.roll_54.roll_mod.minestar.hub.home;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Every player this server has seen, by name — what the invite dropdown offers.
 *
 * <p>There is no vanilla API for "list everyone". {@code GameProfileCache} can answer by name or by
 * id but cannot be enumerated: its backing maps and the {@code GameProfileInfo} type itself are
 * package-private, so reaching them would take an access transformer for what is only ever a
 * convenience list. Instead the ids come from {@code <world>/playerdata}, which has a file per player
 * who has ever logged in, and each is turned into a name by {@link net.minecraft.server.players.GameProfileCache#get(UUID)}
 * — public, and a plain map lookup that never contacts Mojang.
 *
 * <p>The directory listing is done once and cached, because it is disk I/O and the roster ships with
 * every home sync. Joining players are folded in as they arrive, so the list stays current for the
 * case that actually matters — someone logging in while you have the tab open — without re-reading
 * the directory.
 */
public final class KnownPlayers {

    /**
     * How many names the dropdown will carry.
     *
     * <p>A bound on the packet more than on the UI: this rides along with every home sync, and a
     * long-lived server can accumulate thousands of players. Names are at most 16 characters, so this
     * caps the roster at a few kilobytes.
     */
    private static final int MAX_NAMES = 300;

    /** Case-insensitive, so the dropdown reads alphabetically rather than by ASCII case. */
    private static final Set<String> NAMES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private static volatile boolean loaded;

    private KnownPlayers() {}

    /**
     * Names known to this server, alphabetical, minus {@code exclude}.
     *
     * <p>The caller's own name is excluded rather than filtered client-side because inviting yourself
     * is refused anyway ({@code HomeService} answers {@code inviteSelf}), and an option that can only
     * produce an error is not worth offering.
     */
    public static synchronized List<String> namesFor(ServerPlayer viewer) {
        load(viewer.server);
        String self = viewer.getGameProfile().getName();

        List<String> names = new ArrayList<>(Math.min(NAMES.size(), MAX_NAMES));
        for (String name : NAMES) {
            if (name.equalsIgnoreCase(self)) {
                continue;
            }
            if (names.size() == MAX_NAMES) {
                break;
            }
            names.add(name);
        }
        return names;
    }

    /** Folds a joining player in, so the roster is current without re-reading the directory. */
    public static synchronized void remember(ServerPlayer player) {
        NAMES.add(player.getGameProfile().getName());
    }

    private static void load(MinecraftServer server) {
        if (loaded) {
            return;
        }
        loaded = true;

        // Everyone currently online first: they are certain to be resolvable, whereas a profile that
        // has aged out of the cache is not.
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            NAMES.add(online.getGameProfile().getName());
        }

        Path dir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if (!Files.isDirectory(dir)) {
            return;
        }

        Set<UUID> ids = new LinkedHashSet<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.dat")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                try {
                    ids.add(UUID.fromString(name.substring(0, name.length() - ".dat".length())));
                } catch (IllegalArgumentException ignored) {
                    // Not a player file — .dat_old backups and anything else living here.
                }
            }
        } catch (IOException e) {
            RollMod.LOGGER.warn("[Homes] Could not list {} for the invite roster", dir, e);
            return;
        }

        var cache = server.getProfileCache();
        if (cache == null) {
            return;
        }
        for (UUID id : ids) {
            // A miss means the profile aged out of usercache.json, not that the player is unknown.
            // Skipping them is the honest answer: without a name there is nothing to offer.
            cache.get(id).ifPresent(profile -> NAMES.add(profile.getName()));
        }

        RollMod.LOGGER.info("[Homes] Invite roster: {} known player name(s).", NAMES.size());
    }

    /** A fresh world in the same session must not inherit the previous one's roster. */
    public static synchronized void clear() {
        NAMES.clear();
        loaded = false;
    }
}
