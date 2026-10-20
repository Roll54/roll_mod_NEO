package com.roll_54.roll_mod.minestar.friends;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skin textures for players who are not on this server, fetched by the <em>server</em> from its own
 * session service.
 *
 * <p>Why the server: a client handed only a UUID and a name has no {@code textures} property to
 * download from, so it falls back to a default face — which is what every offline friend showed. The
 * server's session service is whichever authlib the server runs with: on the live server that is
 * Minestar's (authlib-injector), so the skins are Minestar's; on a plain server, Mojang's. The
 * property travels to the client with the friends list, and the client downloads the skin from it.
 *
 * <p>Players on this server need no lookup: their profile already carries the property.
 * Lookups block on the network, so they run on the background pool; results are cached for an hour,
 * misses for five minutes so a player with no skin is not asked about on every hub open.
 */
public final class SkinLookup {

    private static final long HIT_TTL_MILLIS = 60 * 60_000L;
    private static final long MISS_TTL_MILLIS = 5 * 60_000L;

    private record Cached(@Nullable Property textures, long at) {
        boolean fresh(long now) {
            return now - at < (textures != null ? HIT_TTL_MILLIS : MISS_TTL_MILLIS);
        }
    }

    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    private SkinLookup() {}

    /** The textures property if it is known right now — online here, or cached — otherwise null. */
    @Nullable
    public static Property get(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            Property property = textures(online.getGameProfile());
            if (property != null) return property;
        }
        Cached cached = CACHE.get(id);
        return cached == null ? null : cached.textures();
    }

    /**
     * Whether {@link #get} already has the answer — the player is online here, or was looked up —
     * including the answer "no skin". False means a {@link #prefetch} is still needed.
     */
    public static boolean known(MinecraftServer server, UUID id) {
        return server.getPlayerList().getPlayer(id) != null || CACHE.containsKey(id);
    }

    /**
     * Starts lookups for every id not already known or in flight, and runs {@code onLoaded} once on
     * the server thread when they have all landed — once per batch, not once per player, so a long
     * friends list costs one re-sync rather than one per friend. Not run when there was nothing to
     * fetch.
     */
    public static void prefetch(MinecraftServer server, Collection<UUID> ids, Runnable onLoaded) {
        long now = System.currentTimeMillis();
        List<CompletableFuture<Void>> lookups = new ArrayList<>();
        for (UUID id : ids) {
            if (server.getPlayerList().getPlayer(id) != null) continue;
            Cached cached = CACHE.get(id);
            if (cached != null && cached.fresh(now)) continue;
            if (!PENDING.add(id)) continue;
            lookups.add(CompletableFuture.runAsync(() -> CACHE.put(id, new Cached(fetch(server, id),
                    System.currentTimeMillis())), Util.backgroundExecutor())
                    .whenComplete((ignored, error) -> PENDING.remove(id)));
        }
        if (lookups.isEmpty()) return;
        CompletableFuture.allOf(lookups.toArray(CompletableFuture[]::new))
                .whenComplete((ignored, error) -> server.execute(onLoaded));
    }

    @Nullable
    private static Property fetch(MinecraftServer server, UUID id) {
        try {
            ProfileResult result = server.getSessionService().fetchProfile(id, false);
            return result == null ? null : textures(result.profile());
        } catch (Exception e) {
            RollMod.LOGGER.debug("[Skins] could not fetch the profile of {}", id, e);
            return null;
        }
    }

    @Nullable
    private static Property textures(GameProfile profile) {
        for (Property property : profile.getProperties().get("textures")) {
            return property;
        }
        return null;
    }
}
