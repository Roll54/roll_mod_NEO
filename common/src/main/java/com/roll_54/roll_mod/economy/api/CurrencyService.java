package com.roll_54.roll_mod.economy.api;

import com.roll_54.roll_mod.economy.currency.model.BalanceEntry;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository;
import com.roll_54.roll_mod.economy.network.payload.SyncBalancePayload;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Reading and moving balances.
 *
 * <p>Two shapes of the same operations: one taking a {@link ServerPlayer}, and one taking a bare
 * {@link UUID} with the server beside it. The store has always been uuid-keyed and offline-safe —
 * the player was only ever needed to send the balance back to a client — so the uuid form works on
 * somebody who is not logged in, and syncs only if they turn out to be. That is what lets an admin
 * pay an offline player and an offer refund a sender who has since left.
 */
public final class CurrencyService {

  private CurrencyService() {}

  /* ------------------------------------- offline-safe, by uuid ------------------------------------ */

  /** Pushes {@code uuid}'s balance to them if they are online, and answers with it either way. */
  public static CompletableFuture<Long> get(MinecraftServer server, UUID uuid, CurrencyType type) {
    return CurrencyRepository.getBalance(uuid, type)
        .thenApplyAsync(
            balance -> {
              ServerPlayer player = online(server, uuid);
              if (player != null) {
                PacketDistributor.sendToPlayer(player, new SyncBalancePayload(balance, type));
              }
              return balance;
            },
            server);
  }

  public static CompletableFuture<Boolean> deposit(
      MinecraftServer server, UUID uuid, CurrencyType type, long amount) {
    if (amount <= 0) return CompletableFuture.completedFuture(false);
    return CurrencyRepository.addBalance(uuid, type, amount)
        .thenComposeAsync(success -> resync(server, uuid, type, success), server);
  }

  public static CompletableFuture<Boolean> withdraw(
      MinecraftServer server, UUID uuid, CurrencyType type, long amount) {
    if (amount <= 0) return CompletableFuture.completedFuture(false);
    return CurrencyRepository.withdraw(uuid, type, amount)
        .thenComposeAsync(success -> resync(server, uuid, type, success), server);
  }

  public static CompletableFuture<Boolean> set(
      MinecraftServer server, UUID uuid, CurrencyType type, long amount) {
    long floored = Math.max(0L, amount);
    return CurrencyRepository.setBalance(uuid, type, floored)
        .thenComposeAsync(success -> resync(server, uuid, type, success), server);
  }

  /**
   * Tells {@code uuid} their new balance when they are online and the write went through.
   *
   * <p>A write that succeeded for an offline player is still a success: there is simply nobody to
   * tell, and they are sent the balance on their next login like everybody else.
   */
  private static CompletableFuture<Boolean> resync(
      MinecraftServer server, UUID uuid, CurrencyType type, boolean success) {
    if (!success) return CompletableFuture.completedFuture(false);
    ServerPlayer player = online(server, uuid);
    if (player == null) return CompletableFuture.completedFuture(true);
    return syncPlayerCurrency(player, type).thenApply(v -> true);
  }

  @Nullable
  private static ServerPlayer online(MinecraftServer server, UUID uuid) {
    return server.getPlayerList().getPlayer(uuid);
  }

  private static CompletableFuture<Long> syncPlayerCurrency(
      ServerPlayer player, CurrencyType type) {
    return CurrencyRepository.getBalance(player.getUUID(), type)
        .thenApplyAsync(
            balance -> {
              PacketDistributor.sendToPlayer(player, new SyncBalancePayload(balance, type));
              return balance;
            },
            player.server);
  }

  public static CompletableFuture<Long> get(ServerPlayer player, CurrencyType type) {
    return syncPlayerCurrency(player, type);
  }

  public static CompletableFuture<Boolean> deposit(
      ServerPlayer player, CurrencyType type, long amount, MinecraftServer server) {
    if (amount <= 0) {
      return CompletableFuture.completedFuture(false);
    }

    return CurrencyRepository.addBalance(player.getUUID(), type, amount)
        .thenComposeAsync(
            success -> {
              if (!success) {
                return CompletableFuture.completedFuture(false);
              }
              return syncPlayerCurrency(player, type).thenApply(v -> true);
            },
            server);
  }

  public static CompletableFuture<Boolean> withdraw(
      ServerPlayer player, CurrencyType type, long amount) {
    if (amount <= 0) {
      return CompletableFuture.completedFuture(false);
    }

    return CurrencyRepository.withdraw(player.getUUID(), type, amount)
        .thenComposeAsync(
            success -> {
              if (!success) {
                return CompletableFuture.completedFuture(false);
              }
              return syncPlayerCurrency(player, type).thenApply(v -> true);
            },
            player.server);
  }

  public static CompletableFuture<Boolean> set(
      ServerPlayer player, CurrencyType type, long amount) {
    if (amount < 0) amount = 0;

    return CurrencyRepository.setBalance(player.getUUID(), type, amount)
        .thenComposeAsync(
            success -> {
              if (!success) {
                return CompletableFuture.completedFuture(false);
              }
              return syncPlayerCurrency(player, type).thenApply(v -> true);
            },
            player.server);
  }

  public static CompletableFuture<Boolean> transfer(
      ServerPlayer from, ServerPlayer to, CurrencyType type, long amount, MinecraftServer server) {
    if (amount <= 0) return CompletableFuture.completedFuture(false);
    if (from.getUUID().equals(to.getUUID())) {
      return CompletableFuture.completedFuture(false);
    }

    return CurrencyRepository.transfer(from.getUUID(), to.getUUID(), type, amount)
        .thenComposeAsync(
            success -> {
              if (!success) {
                return CompletableFuture.completedFuture(false);
              }

              return CompletableFuture.allOf(
                      syncPlayerCurrency(from, type), syncPlayerCurrency(to, type))
                  .thenApply(v -> true);
            },
            server);
  }

  public static CompletableFuture<List<BalanceEntry>> getTop10(CurrencyType type) {
    return CurrencyRepository.getTop10(type);
  }
}
