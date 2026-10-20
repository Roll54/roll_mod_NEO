package com.roll_54.roll_mod.economy.currency.repository;

import static com.roll_54.roll_mod.RollMod.LOGGER;
import static com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository.jdbi;
import static com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository.onDatabase;

import com.roll_54.roll_mod.economy.api.CurrencyCause;
import com.roll_54.roll_mod.economy.currency.audit.CurrencyAudit;
import com.roll_54.roll_mod.economy.currency.audit.CurrencyAudit.Op;
import com.roll_54.roll_mod.economy.currency.ledger.LedgerReason;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.model.MoneyRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;

/**
 * The {@code money_requests} table: one player asking another to pay them.
 *
 * <p>Nothing is held, so most endings — refused, cancelled, lapsed — are a plain delete that moves
 * no money. The one ending that does, {@link #pay}, takes the payer's coins and deletes the row in
 * one transaction, and treats the delete's affected-row count as the claim, as {@link
 * CurrencyOfferRepository} does: two answers racing on one request cannot both pay it.
 */
public final class MoneyRequestRepository {

  /** What came of trying to pay a request. */
  public enum Outcome {
    /** The payer's coins went to the requester and the request is gone. */
    PAID,
    /** The payer does not have the amount. Nothing changed; the request still stands. */
    INSUFFICIENT,
    /** Answered, withdrawn or lapsed meanwhile. Nothing was paid. */
    GONE,
    /** The database refused. Nothing was paid. */
    FAILED
  }

  /** The outcome, and the request as it was when it was read (empty when it was not there). */
  public record Payment(Outcome outcome, Optional<MoneyRequest> request) {}

  private static final String SELECT =
      "SELECT id, requester, requester_name, payer, payer_name, type, amount, expires_at"
          + " FROM money_requests";

  private static final String CREDIT =
      """
          INSERT INTO currency_balances (uuid, type, balance)
          VALUES (:uuid, :type, :amount)
          ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)
      """;

  private MoneyRequestRepository() {}

  /** Writes a new request. False when the database refused it. */
  public static CompletableFuture<Boolean> create(MoneyRequest request) {
    return onDatabase(
        "createMoneyRequest",
        false,
        () ->
            jdbi()
                .withHandle(
                    handle ->
                        handle
                                .createUpdate(
                                    """
                        INSERT INTO money_requests
                            (id, requester, requester_name, payer, payer_name, type, amount, created_at, expires_at)
                        VALUES (:id, :requester, :requesterName, :payer, :payerName, :type, :amount, :createdAt, :expiresAt)
                    """)
                                .bind("id", request.id().toString())
                                .bind("requester", request.requester().toString())
                                .bind("requesterName", request.requesterName())
                                .bind("payer", request.payer().toString())
                                .bind("payerName", request.payerName())
                                .bind("type", request.type().id())
                                .bind("amount", request.amount())
                                .bind("createdAt", System.currentTimeMillis())
                                .bind("expiresAt", request.expiresAt())
                                .execute()
                            > 0));
  }

  /** What {@code payer} has been asked to pay, soonest to lapse first. */
  public static CompletableFuture<List<MoneyRequest>> incoming(UUID payer) {
    return list("moneyRequestsIncoming", "payer", payer);
  }

  /** What {@code requester} has asked others for and not been answered on. */
  public static CompletableFuture<List<MoneyRequest>> outgoing(UUID requester) {
    return list("moneyRequestsOutgoing", "requester", requester);
  }

  private static CompletableFuture<List<MoneyRequest>> list(
      String operation, String column, UUID uuid) {
    return onDatabase(
        operation,
        List.of(),
        () ->
            jdbi()
                .withHandle(
                    handle -> {
                      try (Query query =
                          handle.createQuery(
                              SELECT + " WHERE " + column + " = :uuid ORDER BY expires_at ASC")) {
                        return read(query.bind("uuid", uuid.toString()));
                      }
                    }));
  }

  /**
   * Ends a request without paying it — refused by the payer, or withdrawn by the requester. The
   * result is the request as it was; empty when it was already gone.
   */
  public static CompletableFuture<Optional<MoneyRequest>> delete(UUID id) {
    return onDatabase(
        "deleteMoneyRequest",
        Optional.<MoneyRequest>empty(),
        () ->
            jdbi()
                .inTransaction(
                    handle -> {
                      Optional<MoneyRequest> found = find(handle, id);
                      if (found.isEmpty()) return found;
                      int deleted =
                          handle
                              .createUpdate("DELETE FROM money_requests WHERE id = :id")
                              .bind("id", id.toString())
                              .execute();
                      return deleted == 0 ? Optional.<MoneyRequest>empty() : found;
                    }));
  }

  /** Drops what has lapsed and says what it dropped, so both ends can be told. */
  public static CompletableFuture<List<MoneyRequest>> deleteExpired(long now) {
    return onDatabase(
        "deleteExpiredMoneyRequests",
        List.of(),
        () ->
            jdbi()
                .inTransaction(
                    handle -> {
                      List<MoneyRequest> lapsed;
                      try (Query query =
                          handle.createQuery(SELECT + " WHERE expires_at <= :now LIMIT 256")) {
                        lapsed = read(query.bind("now", now));
                      }
                      for (MoneyRequest request : lapsed) {
                        handle
                            .createUpdate("DELETE FROM money_requests WHERE id = :id")
                            .bind("id", request.id().toString())
                            .execute();
                      }
                      return lapsed;
                    }));
  }

  /**
   * The payer agrees: their coins go to the requester and the request ends, as one transaction.
   *
   * <p>The debit is guarded ({@code balance >= amount}), so a payer who is short is refused with
   * nothing changed and the request left standing — they can top up and answer again. The delete
   * comes after the debit and decides a race: if somebody else ended the row meanwhile, the debit is
   * rolled back.
   */
  public static CompletableFuture<Payment> pay(UUID id, CurrencyCause cause) {
    long audit = CurrencyAudit.begin(Op.REQUEST_PAY, null, 0, null, null,
        cause.withDetail(join(cause.detail(), "money request " + id)));
    return CurrencyAudit.track(audit, payNow(id), payment -> switch (payment.outcome()) {
      case PAID -> payment.request()
          .map(r -> "OK paid " + r.type().id() + " " + r.amount() + " from " + r.payerName() + "("
              + r.payer() + ") to " + r.requesterName() + "(" + r.requester() + ")")
          .orElse("OK");
      case INSUFFICIENT -> "REFUSED (insufficient)";
      case GONE -> "REFUSED (already answered or expired)";
      case FAILED -> "FAILED";
    });
  }

  private static CompletableFuture<Payment> payNow(UUID id) {
    return onDatabase(
            "payMoneyRequest",
            new Payment(Outcome.FAILED, Optional.empty()),
            () -> jdbi().inTransaction(handle -> payOne(handle, id)))
        .thenApply(
            payment -> {
              if (payment.outcome() == Outcome.PAID) {
                payment.request().ifPresent(r -> {
                  CurrencyLedger.record(r.payer(), r.type(), -r.amount(),
                      LedgerReason.TRANSFER, null, 0, r.requester(), r.requesterName());
                  CurrencyLedger.record(r.requester(), r.type(), r.amount(),
                      LedgerReason.TRANSFER, null, 0, r.payer(), r.payerName());
                });
              }
              return payment;
            });
  }

  private static Payment payOne(Handle handle, UUID id) {
    Optional<MoneyRequest> found = find(handle, id);
    if (found.isEmpty() || found.get().expired(System.currentTimeMillis())) {
      return new Payment(Outcome.GONE, found);
    }
    MoneyRequest request = found.get();

    int debited =
        handle
            .createUpdate(
                """
                    UPDATE currency_balances
                    SET balance = balance - :amount
                    WHERE uuid = :uuid AND type = :type AND balance >= :amount
                """)
            .bind("uuid", request.payer().toString())
            .bind("type", request.type().id())
            .bind("amount", request.amount())
            .execute();
    if (debited == 0) return new Payment(Outcome.INSUFFICIENT, found);

    int deleted =
        handle
            .createUpdate("DELETE FROM money_requests WHERE id = :id")
            .bind("id", id.toString())
            .execute();
    if (deleted == 0) {
      // Ended by somebody else between the read and here: the debit must not stand.
      handle.rollback();
      return new Payment(Outcome.GONE, found);
    }

    handle
        .createUpdate(CREDIT)
        .bind("uuid", request.requester().toString())
        .bind("type", request.type().id())
        .bind("amount", request.amount())
        .execute();
    return new Payment(Outcome.PAID, found);
  }

  private static Optional<MoneyRequest> find(Handle handle, UUID id) {
    try (Query query = handle.createQuery(SELECT + " WHERE id = :id")) {
      return read(query.bind("id", id.toString())).stream().findFirst();
    }
  }

  /** Rows to requests, dropping any the build cannot make sense of (unknown currency, bad uuid). */
  private static List<MoneyRequest> read(Query query) {
    return query
        .map(
            (rs, ctx) -> {
              UUID id = uuid(rs.getString("id"));
              UUID requester = uuid(rs.getString("requester"));
              UUID payer = uuid(rs.getString("payer"));
              Optional<CurrencyType> type = CurrencyType.byId(rs.getString("type"));
              if (id == null || requester == null || payer == null || type.isEmpty()) {
                LOGGER.warn("Skipping unreadable money request row '{}'", rs.getString("id"));
                return null;
              }
              return new MoneyRequest(
                  id,
                  requester,
                  rs.getString("requester_name"),
                  payer,
                  rs.getString("payer_name"),
                  type.get(),
                  rs.getLong("amount"),
                  rs.getLong("expires_at"));
            })
        .stream()
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private static String join(String a, String b) {
    return a == null || a.isBlank() ? b : a + "; " + b;
  }

  private static UUID uuid(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException | NullPointerException e) {
      return null;
    }
  }
}
