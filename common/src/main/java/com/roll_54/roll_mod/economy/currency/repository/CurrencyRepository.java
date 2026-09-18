package com.roll_54.roll_mod.economy.currency.repository;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.economy.currency.model.BalanceEntry;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.database.DatabaseManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jdbi.v3.core.Jdbi;

public final class CurrencyRepository {

  private CurrencyRepository() {}

  private static Jdbi jdbi() {
    return DatabaseManager.getInstance().getJdbi();
  }

  public static CompletableFuture<Long> getBalance(UUID uuid, CurrencyType type) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return jdbi()
                .withHandle(
                    handle ->
                        handle
                            .createQuery(
                                "SELECT balance FROM currency_balances WHERE uuid = :uuid AND type = :type")
                            .bind("uuid", uuid.toString())
                            .bind("type", type.id())
                            .mapTo(Long.class)
                            .findOne()
                            .orElse(0L));
          } catch (Exception e) {
            LOGGER.error("DB Error: getBalance", e);
            return 0L;
          }
        },
        DatabaseManager.getInstance().getExecutor());
  }

  public static CompletableFuture<Boolean> setBalance(UUID uuid, CurrencyType type, long amount) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return jdbi()
                .withHandle(
                    handle ->
                        handle
                                .createUpdate(
                                    """
                        INSERT INTO currency_balances (uuid, type, balance)
                        VALUES (:uuid, :type, :amount)
                        ON DUPLICATE KEY UPDATE balance = VALUES(balance)
                    """)
                                .bind("uuid", uuid.toString())
                                .bind("type", type.id())
                                .bind("amount", amount)
                                .execute()
                            > 0);
          } catch (Exception e) {
            LOGGER.error("DB Error: setBalance", e);
            return false;
          }
        },
        DatabaseManager.getInstance().getExecutor());
  }

  public static CompletableFuture<Boolean> addBalance(UUID uuid, CurrencyType type, long amount) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (amount == 0) return true;
          try {
            return jdbi()
                .withHandle(
                    handle ->
                        handle
                                .createUpdate(
                                    """
                        INSERT INTO currency_balances (uuid, type, balance)
                        VALUES (:uuid, :type, :amount)
                        ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)
                    """)
                                .bind("uuid", uuid.toString())
                                .bind("type", type.id())
                                .bind("amount", amount)
                                .execute()
                            > 0);
          } catch (Exception e) {
            LOGGER.error("DB Error: addBalance", e);
            return false;
          }
        },
        DatabaseManager.getInstance().getExecutor());
  }

  public static CompletableFuture<Boolean> withdraw(UUID uuid, CurrencyType type, long amount) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (amount <= 0) return false;
          try {
            return jdbi()
                .withHandle(
                    handle -> {
                      int updated =
                          handle
                              .createUpdate(
                                  """
                        UPDATE currency_balances
                        SET balance = balance - :amount
                        WHERE uuid = :uuid AND type = :type AND balance >= :amount
                    """)
                              .bind("uuid", uuid.toString())
                              .bind("type", type.id())
                              .bind("amount", amount)
                              .execute();
                      return updated > 0;
                    });
          } catch (Exception e) {
            LOGGER.error("DB Error: withdraw", e);
            return false;
          }
        },
        DatabaseManager.getInstance().getExecutor());
  }

  public static CompletableFuture<Boolean> transfer(
      UUID from, UUID to, CurrencyType type, long amount) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (amount <= 0 || from.equals(to)) return false;
          try {
            return jdbi()
                .inTransaction(
                    handle -> {
                      // 1. Withdraw
                      int withdrawn =
                          handle
                              .createUpdate(
                                  """
                        UPDATE currency_balances
                        SET balance = balance - :amount
                        WHERE uuid = :uuid AND type = :type AND balance >= :amount
                    """)
                              .bind("uuid", from.toString())
                              .bind("type", type.id())
                              .bind("amount", amount)
                              .execute();

                      if (withdrawn == 0) return false; // Not enough balance

                      // 2. Add
                      handle
                          .createUpdate(
                              """
                        INSERT INTO currency_balances (uuid, type, balance)
                        VALUES (:uuid, :type, :amount)
                        ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)
                    """)
                          .bind("uuid", to.toString())
                          .bind("type", type.id())
                          .bind("amount", amount)
                          .execute();

                      return true;
                    });
          } catch (Exception e) {
            LOGGER.error("DB Error: transfer", e);
            return false;
          }
        },
        DatabaseManager.getInstance().getExecutor());
  }

  public static CompletableFuture<List<BalanceEntry>> getTop10(CurrencyType type) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return jdbi()
                .withHandle(
                    handle ->
                        handle
                            .createQuery(
                                """
                        SELECT uuid, balance FROM currency_balances
                        WHERE type = :type
                        ORDER BY balance DESC LIMIT 10
                    """)
                            .bind("type", type.id())
                            .mapTo(BalanceEntry.class)
                            .list());
          } catch (Exception e) {
            LOGGER.error("DB Error: getTop10", e);
            return List.of();
          }
        },
        DatabaseManager.getInstance().getExecutor());
  }
}
