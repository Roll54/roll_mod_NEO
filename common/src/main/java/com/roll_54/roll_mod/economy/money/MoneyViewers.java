package com.roll_54.roll_mod.economy.money;

import com.roll_54.roll_mod.economy.api.CurrencyOfferService;
import com.roll_54.roll_mod.economy.api.MoneyRequestService;
import com.roll_54.roll_mod.network.packet.SyncMoneyPacket;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Who has the hub open and therefore wants their money offers and requests kept current.
 *
 * <p>A payload per recipient, like {@code TpaViewers}: the lists are "yours". They live in the
 * database, so a sync is four reads off the server thread and a send back on it.
 */
public final class MoneyViewers {

    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

    private MoneyViewers() {}

    public static void add(ServerPlayer player) {
        VIEWERS.add(player.getUUID());
        syncTo(player);
    }

    public static void remove(UUID player) {
        VIEWERS.remove(player);
    }

    /** Re-sends to whichever of {@code players} has the hub open — both ends of whatever changed. */
    public static void changed(MinecraftServer server, UUID... players) {
        for (UUID id : players) {
            if (!VIEWERS.contains(id)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) syncTo(player);
        }
    }

    public static void syncTo(ServerPlayer player) {
        UUID id = player.getUUID();
        MinecraftServer server = player.server;
        var offersIn = CurrencyOfferService.incoming(id);
        var offersOut = CurrencyOfferService.outgoing(id);
        var requestsIn = MoneyRequestService.incoming(id);
        var requestsOut = MoneyRequestService.outgoing(id);
        CompletableFuture.allOf(offersIn, offersOut, requestsIn, requestsOut).thenRunAsync(() -> {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online == null) return;
            PacketDistributor.sendToPlayer(online, new SyncMoneyPacket(offersIn.join(), offersOut.join(),
                    requestsIn.join(), requestsOut.join()));
        }, server);
    }
}
