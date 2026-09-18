package com.roll_54.roll_mod.minestar.hub.warp;

import com.roll_54.roll_mod.network.packet.SyncWarpsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players with the hub open, and therefore holding a warp list that has to stay current. Mirrors
 * {@code PlotViewers}: pushed on open and again whenever a warp is created or removed.
 */
public final class WarpViewers {

    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

    private WarpViewers() {}

    public static void add(ServerPlayer player) {
        VIEWERS.add(player.getUUID());
        syncTo(player);
    }

    public static void remove(UUID id) {
        VIEWERS.remove(id);
    }

    public static void resync(MinecraftServer server) {
        for (UUID id : VIEWERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) syncTo(player);
        }
    }

    public static void syncTo(ServerPlayer player) {
        // Sorted here rather than on the client, so the popularity order is decided once, on the
        // side that owns the visit counts — see WarpData.sorted().
        PacketDistributor.sendToPlayer(player, new SyncWarpsPacket(
                WarpData.get(player.server).sorted(),
                WarpService.limitFor(player)));
    }
}
