package com.roll_54.roll_mod.economy.currency.ledger;

import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** The money history's filter drop-down. Each entry is the set of reasons it shows. */
public enum HistoryFilter {
    ALL(EnumSet.allOf(LedgerReason.class)),
    SALES(EnumSet.of(LedgerReason.AUCTION_SALE, LedgerReason.AUCTION_FEE, LedgerReason.VENDOR_SALE)),
    PURCHASES(EnumSet.of(LedgerReason.AUCTION_PURCHASE, LedgerReason.AUCTION_BID, LedgerReason.AUCTION_REFUND,
            LedgerReason.VENDOR_PURCHASE)),
    VENDING(EnumSet.of(LedgerReason.VENDOR_SALE, LedgerReason.VENDOR_PURCHASE)),
    AUCTION(EnumSet.of(LedgerReason.AUCTION_SALE, LedgerReason.AUCTION_FEE, LedgerReason.AUCTION_PURCHASE,
            LedgerReason.AUCTION_BID, LedgerReason.AUCTION_REFUND)),
    REWARDS(EnumSet.of(LedgerReason.PLAYTIME, LedgerReason.DAILY, LedgerReason.QUEST, LedgerReason.LETTER));

    private final Set<LedgerReason> reasons;

    HistoryFilter(Set<LedgerReason> reasons) {
        this.reasons = reasons;
    }

    public Set<LedgerReason> reasons() {
        return reasons;
    }

    public Component title() {
        return Component.translatable("gui.roll_mod.hub.history.filter." + name().toLowerCase(Locale.ROOT));
    }
}
