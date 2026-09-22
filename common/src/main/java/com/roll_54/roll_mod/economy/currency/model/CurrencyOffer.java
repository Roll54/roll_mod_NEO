package com.roll_54.roll_mod.economy.currency.model;

import java.util.UUID;

/**
 * Money one player has offered another, waiting to be answered.
 *
 * <p>The amount is already out of the sender's balance: {@code CurrencyOfferService} withdraws it
 * before the row exists and pays it to whoever the answer says — the recipient on an accept, the
 * sender back on a deny or an expiry. Nothing else may delete a row, because a deleted row is money
 * that stopped existing.
 *
 * <p>Both names are stored rather than looked up, so an offer can be listed and answered by name
 * while the other party is offline — which is the point of offers surviving a logout at all.
 */
public record CurrencyOffer(UUID id, UUID sender, String senderName, UUID recipient,
                            String recipientName, CurrencyType type, long amount, long expiresAt) {

    public long remainingMillis(long now) {
        return Math.max(0L, expiresAt - now);
    }

    public boolean expired(long now) {
        return now >= expiresAt;
    }
}
