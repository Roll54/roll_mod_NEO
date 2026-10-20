package com.roll_54.roll_mod.economy.network.client;

import com.roll_54.roll_mod.economy.currency.model.CurrencyOffer;
import com.roll_54.roll_mod.economy.currency.model.MoneyRequest;
import java.util.List;
import java.util.UUID;

/**
 * The viewer's money offers and requests, as the last {@code SyncMoneyPacket} had them.
 *
 * <p>Offers: money sent with an accept/deny pending. Requests: money asked for, nothing held.
 * "Incoming" is what the viewer has to answer either way.
 */
public final class ClientMoneyCache {

    public static List<CurrencyOffer> OFFERS_IN = List.of();
    public static List<CurrencyOffer> OFFERS_OUT = List.of();
    public static List<MoneyRequest> REQUESTS_IN = List.of();
    public static List<MoneyRequest> REQUESTS_OUT = List.of();
    /** Bumped on every sync, so a screen can rebuild only when something arrived. */
    public static int VERSION = 0;

    private ClientMoneyCache() {}

    public static boolean anyIncoming() {
        return !OFFERS_IN.isEmpty() || !REQUESTS_IN.isEmpty();
    }

    /** True when anything waits between the viewer and {@code other}, either way. */
    public static boolean involves(UUID other) {
        return OFFERS_IN.stream().anyMatch(o -> o.sender().equals(other))
                || OFFERS_OUT.stream().anyMatch(o -> o.recipient().equals(other))
                || REQUESTS_IN.stream().anyMatch(r -> r.requester().equals(other))
                || REQUESTS_OUT.stream().anyMatch(r -> r.payer().equals(other));
    }
}
