package com.roll_54.roll_mod.minestar.op;

import com.roll_54.roll_mod.network.packet.SyncOperatorsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who has the hub open and may see the operator roster.
 *
 * <p>Everyone watching gets a payload; everyone who is not an administrator gets an empty one. The
 * panel is built for all of them either way — the hub's tree may not vary by permission — so the
 * emptiness is what hides it.
 */
public final class OperatorViewers {

    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

    private OperatorViewers() {}

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
        List<OperatorEntry> entries = OperatorStore.canManage(player)
                ? OperatorStore.entries()
                : List.of();
        PacketDistributor.sendToPlayer(player, new SyncOperatorsPacket(entries));
    }
}
