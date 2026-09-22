package com.roll_54.roll_mod.minestar.tpa;

import com.roll_54.roll_mod.network.packet.SyncTpaPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who has the hub open and therefore wants request updates.
 *
 * <p>A payload per recipient, like {@code HomeViewers}: the list is "your requests", so there is
 * nothing to broadcast.
 */
public final class TpaViewers {

    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

    private TpaViewers() {}

    public static void add(ServerPlayer player) {
        VIEWERS.add(player.getUUID());
        syncTo(player);
    }

    public static void remove(UUID player) {
        VIEWERS.remove(player);
    }

    public static void resync(MinecraftServer server) {
        for (UUID id : VIEWERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) syncTo(player);
        }
    }

    public static void syncTo(ServerPlayer player) {
        Map<UUID, String> online = new LinkedHashMap<>();
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            online.put(other.getUUID(), other.getGameProfile().getName());
        }

        PacketDistributor.sendToPlayer(player, new SyncTpaPacket(
                TpaService.incoming(player.getUUID()),
                TpaService.outgoing(player.getUUID()),
                online,
                TpaSettings.mode(player.getUUID())));
    }
}
