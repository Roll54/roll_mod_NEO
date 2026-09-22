package com.roll_54.roll_mod.minestar.kits;

import com.roll_54.roll_mod.network.packet.SyncKitsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who has the hub open and therefore wants kit updates.
 *
 * <p>Built per recipient rather than broadcast, like {@code HomeViewers} and unlike
 * {@code WarpViewers}: which kits a player may see depends on their permissions, and their
 * cooldowns are their own.
 */
public final class KitViewers {

    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

    private KitViewers() {}

    public static void add(ServerPlayer player) {
        VIEWERS.add(player.getUUID());
        syncTo(player);
    }

    public static void remove(UUID player) {
        VIEWERS.remove(player);
    }

    /** Pushes a fresh list to everyone watching; call after anything that changes a kit. */
    public static void resync(MinecraftServer server) {
        for (UUID id : VIEWERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) syncTo(player);
        }
    }

    public static void syncTo(ServerPlayer player) {
        List<KitView> views = new ArrayList<>();
        for (Kit kit : KitService.visibleTo(player)) {
            views.add(KitView.of(kit, KitService.remainingMillis(player, kit)));
        }
        PacketDistributor.sendToPlayer(player, new SyncKitsPacket(views));
    }
}
