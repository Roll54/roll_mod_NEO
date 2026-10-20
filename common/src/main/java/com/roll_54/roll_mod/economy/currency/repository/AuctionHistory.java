package com.roll_54.roll_mod.economy.currency.repository;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionAction;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionHistoryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A player's Auction House history ({@code auction_history}, migration V5): written by {@code
 * AuctionManager} after every listing, cancel, purchase, bid, refund, win, expiry and claim, read by
 * the Auction House's History panel.
 *
 * <p>Lives beside {@link CurrencyLedger} for the same database plumbing, and follows the same rule:
 * history only. The listings and claims are still {@code AuctionData}, the money is still {@code
 * currency_balances}; a row is written after the action went through, on the database executor, and
 * a failure to write one is logged and otherwise ignored — it can never fail or undo the action.
 *
 * <p>Unlike the money ledger the item is kept as its registry id, not a display name: the panel
 * shows the item's icon, and resolves the name on the client in the reader's own language.
 */
public final class AuctionHistory {

    private AuctionHistory() {}

    /**
     * Records one action. Fire-and-forget: never throws, and the row may land after the call returns.
     *
     * @param listing null for a claim, which belongs to no listing any more
     * @param item    the stack acted on; its count is ignored in favour of {@code count}
     * @param price   the money involved, 0 when none
     */
    public static void record(UUID player, AuctionAction action, @Nullable UUID listing,
                              @Nullable ItemStack item, int count, long price,
                              @Nullable UUID other, @Nullable String otherName) {
        try {
            String itemId = item == null || item.isEmpty() ? null
                    : clip(BuiltInRegistries.ITEM.getKey(item.getItem()).toString(), 256);
            String name = otherName == null || otherName.isEmpty() ? null : clip(otherName, 16);
            long now = System.currentTimeMillis();
            CurrencyRepository.onDatabase("auctionHistory.record", false, () ->
                    CurrencyRepository.jdbi().withHandle(handle -> handle.createUpdate("""
                            INSERT INTO auction_history
                                (uuid, action, listing_id, item, item_count, price, other_uuid, other_name, created_at)
                            VALUES (:uuid, :action, :listing, :item, :count, :price, :other, :otherName, :at)
                            """)
                            .bind("uuid", player.toString())
                            .bind("action", action.name())
                            .bind("listing", listing == null ? null : listing.toString())
                            .bind("item", itemId)
                            .bind("count", count)
                            .bind("price", price)
                            .bind("other", other == null ? null : other.toString())
                            .bind("otherName", name)
                            .bind("at", now)
                            .execute() > 0));
        } catch (RuntimeException e) {
            // A history row is never worth the action it describes.
            RollMod.LOGGER.error("[Economy] could not queue an auction history row", e);
        }
    }

    /** The newest {@code limit} rows of a player's auction history. */
    public static CompletableFuture<List<AuctionHistoryEntry>> history(UUID player, int limit) {
        return CurrencyRepository.onDatabase("auctionHistory.history", List.of(), () ->
                CurrencyRepository.jdbi().withHandle(handle -> handle.createQuery("""
                        SELECT action, item, item_count, price, other_name, created_at FROM auction_history
                        WHERE uuid = :uuid
                        ORDER BY created_at DESC, id DESC LIMIT :limit
                        """)
                        .bind("uuid", player.toString())
                        .bind("limit", limit)
                        .map((rs, ctx) -> new AuctionHistoryEntry(
                                AuctionAction.byName(rs.getString("action")),
                                orEmpty(rs.getString("item")),
                                rs.getInt("item_count"),
                                rs.getLong("price"),
                                orEmpty(rs.getString("other_name")),
                                rs.getLong("created_at")))
                        .list()));
    }

    private static String clip(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static String orEmpty(@Nullable String text) {
        return text == null ? "" : text;
    }
}
