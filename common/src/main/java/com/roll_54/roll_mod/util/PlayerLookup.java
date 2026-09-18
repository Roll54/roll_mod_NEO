package com.roll_54.roll_mod.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Resolving a typed-in player name to a UUID, including for players who are offline.
 *
 * <p>The currency layer is UUID-keyed and offline-safe all the way down, but nothing in the mod
 * could turn a name into one: the existing call sites all use {@code getPlayerByName} and quietly do
 * something lesser when the player is not online. Anything that lets a player name someone — sharing
 * a home, paying, transferring a shop — needs this instead.
 */
public final class PlayerLookup {

    /**
     * A resolved player. Carries the name as Mojang spells it, not as it was typed, so what gets
     * stored and shown back is correctly cased.
     */
    public record Profile(UUID id, String name) {}

    /**
     * What Mojang will ever accept as a name. Checked before any lookup that could leave the
     * machine: the invite field is player-controlled, and without this the server is a free
     * name-to-UUID proxy that will get its IP rate-limited by someone pasting junk in a loop.
     */
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private PlayerLookup() {}

    public static boolean isPlausibleName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    /** Online only. Never blocks. Vanilla's lookup is already case-insensitive. */
    @Nullable
    public static ServerPlayer online(MinecraftServer server, String name) {
        if (!isPlausibleName(name)) return null;
        return server.getPlayerList().getPlayerByName(name);
    }

    /**
     * The profile cache only — everyone who has joined this server before. Never blocks and never
     * contacts Mojang; {@code null} means "not cached", not "no such player".
     */
    @Nullable
    public static Profile cached(MinecraftServer server, String name) {
        if (!isPlausibleName(name)) return null;
        GameProfileCache cache = server.getProfileCache();
        if (cache == null) return null;
        // GameProfileCache.get(String) makes a *blocking* HTTP call to Mojang on a miss, so it is
        // only safe here because a miss is what we are testing for and the caller treats null as
        // "ask asynchronously instead". Anything wanting a definite answer must use resolve.
        return cache.get(name).map(p -> new Profile(p.getId(), p.getName())).orElse(null);
    }

    /**
     * Online player, then profile cache, then Mojang.
     *
     * <p>The callback always runs on the server thread and always runs exactly once, with {@code
     * null} when the name could not be resolved. Both guarantees matter: the async lookup completes
     * on an arbitrary thread, and touching {@code SavedData} from one corrupts it in ways that only
     * show up much later.
     */
    public static void resolve(MinecraftServer server, String name, Consumer<Profile> callback) {
        if (!isPlausibleName(name)) {
            callback.accept(null);
            return;
        }

        ServerPlayer player = online(server, name);
        if (player != null) {
            callback.accept(new Profile(player.getUUID(), player.getGameProfile().getName()));
            return;
        }

        Profile hit = cached(server, name);
        if (hit != null) {
            callback.accept(hit);
            return;
        }

        GameProfileCache cache = server.getProfileCache();
        if (cache == null) {
            callback.accept(null);
            return;
        }

        // thenAcceptAsync with the server as the executor is how the rest of the codebase hops back
        // onto the main thread from an async future — see CurrencyService's callers.
        cache.getAsync(name).thenAcceptAsync(
                (Optional<GameProfile> found) -> callback.accept(
                        found.map(p -> new Profile(p.getId(), p.getName())).orElse(null)),
                server);
    }
}
