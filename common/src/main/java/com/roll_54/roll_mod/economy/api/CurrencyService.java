package com.roll_54.roll_mod.economy.api;

import com.roll_54.roll_mod.economy.currency.model.BalanceEntry;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository;
import com.roll_54.roll_mod.economy.network.payload.SyncBalancePayload;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CurrencyService {

  private CurrencyService() {}

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
