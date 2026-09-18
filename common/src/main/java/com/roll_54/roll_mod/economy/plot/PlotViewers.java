package com.roll_54.roll_mod.economy.plot;

import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.plot.network.SyncPlotsPacket;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tracks players with the plot-purchase UI open and pushes them fresh {@link SyncPlotsPacket}
 * snapshots — on open and whenever a purchase changes the board. Mirrors {@code AuctionManager}'s
 * viewer mechanism. The balance read is async, so the snapshot (which bakes in price/affordability)
 * is built inside the completion on the server thread.
 */
public final class PlotViewers {

  private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

  private PlotViewers() {}

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
      if (player != null) {
        syncTo(player);
      }
    }
  }

  public static void syncTo(ServerPlayer player) {
    CurrencyService.get(player, CurrencyType.MAIN)
        .thenAcceptAsync(
            balance ->
                PacketDistributor.sendToPlayer(
                    player, new SyncPlotsPacket(PlotService.snapshot(player, balance))),
            player.server);
  }
}
