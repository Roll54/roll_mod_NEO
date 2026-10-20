package com.roll_54.roll_mod.minestar.friends;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who has the hub open and therefore wants the friends list kept current. Same shape as
 * {@code TpaViewers}: opening the hub fetches (or re-sends a fresh) list, and a join or leave anywhere
 * re-sends it so the "online here" marks stay true.
 */
public final class FriendViewers {

    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

    private FriendViewers() {}

    public static void add(ServerPlayer player) {
        VIEWERS.add(player.getUUID());
        FriendsService.refresh(player, false);
    }

    public static void remove(UUID player) {
        VIEWERS.remove(player);
        FriendsService.forget(player);
    }

    /** Re-sends every viewer's list without refetching — only this server's flags changed. */
    public static void resync(MinecraftServer server) {
        for (UUID id : VIEWERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) FriendsService.syncTo(player);
        }
    }
}
