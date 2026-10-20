package com.roll_54.roll_mod.economy.currency.repository;

import com.roll_54.roll_mod.economy.currency.ledger.HistoryFilter;
import com.roll_54.roll_mod.economy.currency.ledger.LedgerEntry;
import com.roll_54.roll_mod.economy.currency.ledger.LedgerReason;
import com.roll_54.roll_mod.economy.currency.model.BalanceEntry;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The money history ({@code currency_ledger}, migration V3): written at every place money moves for
 * a reason a player would want to look back on, read by the Home tab's history and by the "top
 * earners" leaderboard.
 *
 * <p>History only. The balance is still {@code currency_balances}; a row is written after the money
 * has moved, on the same database executor, and a failure to write one is logged and otherwise
 * ignored — it can never fail or undo the payment it describes.
 */
public final class CurrencyLedger {

    private CurrencyLedger() {}

    /** A movement with no item and no other party — a reward, a fee, an admin change. */
    public static void record(UUID player, CurrencyType type, long delta, LedgerReason reason) {
        record(player, type, delta, reason, null, 0, null, null);
    }

    /** A trade: what changed hands and with whom. */
    public static void record(UUID player, CurrencyType type, long delta, LedgerReason reason,
                              @Nullable ItemStack item, int count, @Nullable UUID other, @Nullable String otherName) {
        if (delta == 0) return;
        String itemName = item == null || item.isEmpty() ? null : clip(item.getHoverName().getString(), 256);
        String name = otherName == null ? null : clip(otherName, 16);
        long now = System.currentTimeMillis();
        CurrencyRepository.onDatabase("ledger.record", false, () ->
                CurrencyRepository.jdbi().withHandle(handle -> handle.createUpdate("""
                        INSERT INTO currency_ledger
                            (uuid, type, delta, reason, item, item_count, other_uuid, other_name, created_at)
                        VALUES (:uuid, :type, :delta, :reason, :item, :count, :other, :otherName, :at)
                        """)
                        .bind("uuid", player.toString())
                        .bind("type", type.id())
                        .bind("delta", delta)
                        .bind("reason", reason.name())
                        .bind("item", itemName)
                        .bind("count", count)
                        .bind("other", other == null ? null : other.toString())
                        .bind("otherName", name)
                        .bind("at", now)
                        .execute() > 0));
    }

    /**
     * Records {@code delta} for {@code player} once {@code operation} — a deposit, withdrawal or
     * transfer — reports success, and hands back its result unchanged. For call sites that only
     * need a line in the history and no item or other party.
     */
    public static CompletableFuture<Boolean> logged(CompletableFuture<Boolean> operation, UUID player,
                                                    CurrencyType type, long delta, LedgerReason reason) {
        return operation.thenApply(ok -> {
            if (Boolean.TRUE.equals(ok)) record(player, type, delta, reason);
            return ok;
        });
    }

    /** The newest {@code limit} rows of a player's history that the filter shows. */
    public static CompletableFuture<List<LedgerEntry>> history(UUID player, CurrencyType type,
                                                               HistoryFilter filter, int limit) {
        List<String> reasons = new ArrayList<>();
        for (LedgerReason reason : filter.reasons()) reasons.add(reason.name());
        return CurrencyRepository.onDatabase("ledger.history", List.of(), () ->
                CurrencyRepository.jdbi().withHandle(handle -> handle.createQuery("""
                        SELECT delta, reason, item, item_count, other_name, created_at FROM currency_ledger
                        WHERE uuid = :uuid AND type = :type AND reason IN (<reasons>)
                        ORDER BY created_at DESC, id DESC LIMIT :limit
                        """)
                        .bind("uuid", player.toString())
                        .bind("type", type.id())
                        .bindList("reasons", reasons)
                        .bind("limit", limit)
                        .map((rs, ctx) -> new LedgerEntry(
                                rs.getLong("delta"),
                                LedgerReason.byName(rs.getString("reason")),
                                orEmpty(rs.getString("item")),
                                rs.getInt("item_count"),
                                orEmpty(rs.getString("other_name")),
                                rs.getLong("created_at")))
                        .list()));
    }

    /**
     * Who earned most from selling to other players since {@code sinceMillis}: auction sales (already
     * net of the house's fee) and vending sales. The {@link BalanceEntry} balance is that sum.
     */
    public static CompletableFuture<List<BalanceEntry>> topEarners(CurrencyType type, long sinceMillis, int limit) {
        List<String> reasons = new ArrayList<>();
        for (LedgerReason reason : LedgerReason.values()) {
            if (reason.trade()) reasons.add(reason.name());
        }
        return CurrencyRepository.onDatabase("ledger.topEarners", List.of(), () ->
                CurrencyRepository.jdbi().withHandle(handle -> handle.createQuery("""
                        SELECT uuid, SUM(delta) AS balance FROM currency_ledger
                        WHERE type = :type AND created_at >= :since AND reason IN (<reasons>)
                        GROUP BY uuid HAVING SUM(delta) > 0
                        ORDER BY balance DESC LIMIT :limit
                        """)
                        .bind("type", type.id())
                        .bind("since", sinceMillis)
                        .bindList("reasons", reasons)
                        .bind("limit", limit)
                        .map(BalanceEntry.MAPPER)
                        .list()));
    }

    /** Season reset: forgets every row of one currency, alongside the balances' own wipe. */
    public static CompletableFuture<Integer> wipe(CurrencyType type) {
        return CurrencyRepository.onDatabase("ledger.wipe", 0, () ->
                CurrencyRepository.jdbi().withHandle(handle -> handle
                        .createUpdate("DELETE FROM currency_ledger WHERE type = :type")
                        .bind("type", type.id())
                        .execute()));
    }

    /** One player's trade income since {@code sinceMillis}: their own line on the earners board. */
    public static CompletableFuture<Long> earnings(UUID player, CurrencyType type, long sinceMillis) {
        List<String> reasons = new ArrayList<>();
        for (LedgerReason reason : LedgerReason.values()) {
            if (reason.trade()) reasons.add(reason.name());
        }
        return CurrencyRepository.onDatabase("ledger.earnings", 0L, () ->
                CurrencyRepository.jdbi().withHandle(handle -> handle.createQuery(
                                "SELECT COALESCE(SUM(delta), 0) FROM currency_ledger"
                                        + " WHERE uuid = :uuid AND type = :type AND created_at >= :since"
                                        + " AND reason IN (<reasons>)")
                        .bind("uuid", player.toString())
                        .bind("type", type.id())
                        .bind("since", sinceMillis)
                        .bindList("reasons", reasons)
                        .mapTo(Long.class)
                        .one()));
    }

    private static String clip(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static String orEmpty(@Nullable String text) {
        return text == null ? "" : text;
    }
}
