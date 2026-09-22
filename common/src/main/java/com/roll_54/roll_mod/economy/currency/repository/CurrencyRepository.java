package com.roll_54.roll_mod.economy.currency.repository;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.economy.currency.model.BalanceEntry;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.database.DatabaseManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.jdbi.v3.core.Jdbi;

public final class CurrencyRepository {

  /** So a session without a database logs the reason once instead of once per query. */
  private static final AtomicBoolean UNAVAILABLE_LOGGED = new AtomicBoolean();

  private CurrencyRepository() {}

  /** Package-private: {@link CurrencyOfferRepository} runs its transactions on the same handle. */
  static Jdbi jdbi() {
    return DatabaseManager.getInstance().getJdbi();
  }

  /**
   * Runs {@code body} on the database executor, returning {@code fallback} instead of throwing when
   * there is no database to run it on.
   *
   * <p>Every caller is on the server thread, where an exception is not a failed purchase but a
   * failed tick — the login balance read used to take the whole join down with it. The executor is
   * read once and submission is guarded, because {@code shutdown()} can land between the two.
   */
  static <T> CompletableFuture<T> onDatabase(
      String operation, T fallback, Supplier<T> body) {
    ExecutorService executor = DatabaseManager.getInstance().getExecutor();
    if (executor == null) {
      return unavailable(operation, fallback);
    }
    try {
      return CompletableFuture.supplyAsync(
          () -> {
            try {
              return body.get();
            } catch (Exception e) {
              LOGGER.error("DB Error: {}", operation, e);
              return fallback;
            }
          },
          executor);
    } catch (RejectedExecutionException e) {
      return unavailable(operation, fallback);
    }
  }

  private static <T> CompletableFuture<T> unavailable(String operation, T fallback) {
    if (UNAVAILABLE_LOGGED.compareAndSet(false, true)) {
      LOGGER.warn(
          "Currency database is unavailable; '{}' and any later query return defaults.", operation);
    } else {
      LOGGER.debug("Currency database is unavailable; '{}' skipped.", operation);
    }
    return CompletableFuture.completedFuture(fallback);
  }

  public static CompletableFuture<Long> getBalance(UUID uuid, CurrencyType type) {
    return onDatabase(
        "getBalance",
        0L,
        () ->
            jdbi()
                .withHandle(
                    handle ->
                        handle
                            .createQuery(
                                "SELECT balance FROM currency_balances WHERE uuid = :uuid AND type = :type")
                            .bind("uuid", uuid.toString())
                            .bind("type", type.id())
                            .mapTo(Long.class)
                            .findOne()
                            .orElse(0L)));
  }

  public static CompletableFuture<Boolean> setBalance(UUID uuid, CurrencyType type, long amount) {
    return onDatabase(
        "setBalance",
        false,
        () ->
            jdbi()
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
                            > 0));
  }

  public static CompletableFuture<Boolean> addBalance(UUID uuid, CurrencyType type, long amount) {
    if (amount == 0) return CompletableFuture.completedFuture(true);
    return onDatabase(
        "addBalance",
        false,
        () ->
            jdbi()
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
                            > 0));
  }

  public static CompletableFuture<Boolean> withdraw(UUID uuid, CurrencyType type, long amount) {
    if (amount <= 0) return CompletableFuture.completedFuture(false);
    return onDatabase(
        "withdraw",
        false,
        () ->
            jdbi()
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
                    }));
  }

  public static CompletableFuture<Boolean> transfer(
      UUID from, UUID to, CurrencyType type, long amount) {
    if (amount <= 0 || from.equals(to)) return CompletableFuture.completedFuture(false);
    return onDatabase(
        "transfer",
        false,
        () ->
            jdbi()
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
                    }));
  }

  public static CompletableFuture<List<BalanceEntry>> getTop10(CurrencyType type) {
    return onDatabase(
        "getTop10",
        List.of(),
        () ->
            jdbi()
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
                            .list()));
  }
}
