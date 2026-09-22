package com.roll_54.roll_mod.economy.currency.repository;

import static com.roll_54.roll_mod.RollMod.LOGGER;
import static com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository.jdbi;
import static com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository.onDatabase;

import com.roll_54.roll_mod.economy.currency.model.CurrencyOffer;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;

/**
 * The {@code currency_offers} table: money that has left a sender and is waiting for an answer.
 *
 * <p>Every method that ends an offer does the deletion and the payment in one transaction, and
 * treats the deletion's affected-row count as the claim. Two players cannot both be paid by the same
 * offer that way, and no path can delete a row without paying the amount to somebody — an offer that
 * vanished on its own would be money destroyed.
 *
 * <p>Offers outlive restarts, unlike teleport requests ({@code TpaService} keeps those in memory on
 * purpose). They have to: the money is already held, so forgetting one loses it.
 */
public final class CurrencyOfferRepository {

  /** Who an ending offer pays. */
  public enum Payee {
    /** An accept: the money completes its journey. */
    RECIPIENT,
    /** A deny or an expiry: the money goes home. */
    SENDER
  }

  /** What came of trying to hold money for an offer. */
  public enum Hold {
    /** The money is out of the sender's balance and the offer exists. */
    HELD,
    /** The sender does not have it. Nothing was written. */
    INSUFFICIENT,
    /** The database refused. Nothing was written, and the money is still theirs. */
    FAILED
  }

  /** How many lapsed offers one sweep refunds. Bounds the work a single tick can take on. */
  private static final int EXPIRY_BATCH = 64;

  private static final String SELECT =
      "SELECT id, sender, sender_name, recipient, recipient_name, type, amount, expires_at"
          + " FROM currency_offers";

  /** The same upsert {@code CurrencyRepository.addBalance} uses, run inside a transaction. */
  private static final String CREDIT =
      """
          INSERT INTO currency_balances (uuid, type, balance)
          VALUES (:uuid, :type, :amount)
          ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)
      """;

  private CurrencyOfferRepository() {}

  /**
   * Takes the amount out of the sender's balance and writes the offer, as one transaction.
   *
   * <p>Both or neither. Debiting first and inserting after would leave a window — a crash, a
   * connection dropped mid-pair — in which the money had left the sender with no row anywhere saying
   * who was owed it, and nothing afterwards could tell that from a spend.
   */
  public static CompletableFuture<Hold> hold(CurrencyOffer offer) {
    return onDatabase(
        "holdOffer",
        Hold.FAILED,
        () ->
            jdbi()
                .inTransaction(
                    handle -> {
                      int debited =
                          handle
                              .createUpdate(
                                  """
                        UPDATE currency_balances
                        SET balance = balance - :amount
                        WHERE uuid = :uuid AND type = :type AND balance >= :amount
                    """)
                              .bind("uuid", offer.sender().toString())
                              .bind("type", offer.type().id())
                              .bind("amount", offer.amount())
                              .execute();
                      // No row, or not enough in it. Either way there is nothing to hold.
                      if (debited == 0) return Hold.INSUFFICIENT;

                      handle
                          .createUpdate(
                              """
                        INSERT INTO currency_offers
                            (id, sender, sender_name, recipient, recipient_name, type, amount, expires_at)
                        VALUES (:id, :sender, :senderName, :recipient, :recipientName, :type, :amount, :expiresAt)
                    """)
                          .bind("id", offer.id().toString())
                          .bind("sender", offer.sender().toString())
                          .bind("senderName", offer.senderName())
                          .bind("recipient", offer.recipient().toString())
                          .bind("recipientName", offer.recipientName())
                          .bind("type", offer.type().id())
                          .bind("amount", offer.amount())
                          .bind("expiresAt", offer.expiresAt())
                          .execute();
                      return Hold.HELD;
                    }));
  }

  /** What is waiting for {@code recipient} to answer, soonest to lapse first. */
  public static CompletableFuture<List<CurrencyOffer>> incoming(UUID recipient) {
    return list("offersIncoming", "recipient", recipient);
  }

  /** What {@code sender} is still owed an answer on. */
  public static CompletableFuture<List<CurrencyOffer>> outgoing(UUID sender) {
    return list("offersOutgoing", "sender", sender);
  }

  private static CompletableFuture<List<CurrencyOffer>> list(
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
   * Ends one offer and pays it.
   *
   * <p>The result is the offer as it was, so the caller can say what happened to whom; empty means
   * somebody else got there first — the row was already answered, or swept as expired — and in that
   * case nothing was paid.
   */
  public static CompletableFuture<Optional<CurrencyOffer>> claim(UUID id, Payee payee) {
    return onDatabase(
        "claimOffer",
        Optional.empty(),
        () -> jdbi().inTransaction(handle -> claimOne(handle, id, payee)));
  }

  /**
   * Refunds everything that has lapsed, and says what it refunded so the senders can be told.
   *
   * <p>One transaction per offer rather than one for the batch: a single unreadable row would
   * otherwise roll back the refunds of every other sender in it.
   */
  public static CompletableFuture<List<CurrencyOffer>> claimExpired(long now) {
    return onDatabase(
        "claimExpiredOffers",
        List.of(),
        () -> {
          List<UUID> lapsed =
              jdbi()
                  .withHandle(
                      handle -> {
                        try (Query query =
                            handle.createQuery(
                                "SELECT id FROM currency_offers WHERE expires_at <= :now"
                                    + " ORDER BY expires_at ASC LIMIT "
                                    + EXPIRY_BATCH)) {
                          return query.bind("now", now).mapTo(String.class).list().stream()
                              .map(CurrencyOfferRepository::uuid)
                              .filter(java.util.Objects::nonNull)
                              .toList();
                        }
                      });

          List<CurrencyOffer> refunded = new ArrayList<>();
          for (UUID id : lapsed) {
            jdbi()
                .inTransaction(handle -> claimOne(handle, id, Payee.SENDER))
                .ifPresent(refunded::add);
          }
          return refunded;
        });
  }

  /**
   * The body of every ending: read the offer, delete it, pay whoever the answer names.
   *
   * <p>The delete's affected-row count is the claim. Two callers racing to answer the same offer
   * both read it, but only one deletes a row, and only that one pays.
   */
  private static Optional<CurrencyOffer> claimOne(Handle handle, UUID id, Payee payee) {
    Optional<CurrencyOffer> found;
    try (Query query = handle.createQuery(SELECT + " WHERE id = :id")) {
      found = read(query.bind("id", id.toString())).stream().findFirst();
    }
    if (found.isEmpty()) return Optional.empty();
    CurrencyOffer offer = found.get();

    int deleted =
        handle
            .createUpdate("DELETE FROM currency_offers WHERE id = :id")
            .bind("id", id.toString())
            .execute();
    if (deleted == 0) return Optional.empty();

    UUID payTo = payee == Payee.RECIPIENT ? offer.recipient() : offer.sender();
    handle
        .createUpdate(CREDIT)
        .bind("uuid", payTo.toString())
        .bind("type", offer.type().id())
        .bind("amount", offer.amount())
        .execute();

    return Optional.of(offer);
  }

  /**
   * Rows to offers, dropping any the build cannot make sense of.
   *
   * <p>A row naming an unknown currency or a malformed uuid is left in the table on purpose. It is
   * money held for somebody, and skipping it keeps it claimable by a later build that understands
   * it; deleting it would be the one thing this class never does.
   */
  private static List<CurrencyOffer> read(Query query) {
    return query
        .map(
            (rs, ctx) -> {
              UUID id = uuid(rs.getString("id"));
              UUID sender = uuid(rs.getString("sender"));
              UUID recipient = uuid(rs.getString("recipient"));
              Optional<CurrencyType> type = CurrencyType.byId(rs.getString("type"));
              if (id == null || sender == null || recipient == null || type.isEmpty()) {
                LOGGER.warn("Skipping unreadable currency offer row '{}'", rs.getString("id"));
                return null;
              }
              return new CurrencyOffer(
                  id,
                  sender,
                  rs.getString("sender_name"),
                  recipient,
                  rs.getString("recipient_name"),
                  type.get(),
                  rs.getLong("amount"),
                  rs.getLong("expires_at"));
            })
        .stream()
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private static UUID uuid(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException | NullPointerException e) {
      return null;
    }
  }
}
