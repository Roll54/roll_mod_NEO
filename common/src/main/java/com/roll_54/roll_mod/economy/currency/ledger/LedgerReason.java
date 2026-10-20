package com.roll_54.roll_mod.economy.currency.ledger;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Why money moved, as stored in {@code currency_ledger.reason} and shown in the history. Stored by
 * name, so values may be added freely but never renamed.
 */
public enum LedgerReason {
    AUCTION_SALE(Group.AUCTION, true),
    /** What the auction house kept of a sale. Informational: the sale row already holds the payout. */
    AUCTION_FEE(Group.AUCTION, false),
    AUCTION_PURCHASE(Group.AUCTION, false),
    AUCTION_BID(Group.AUCTION, false),
    AUCTION_REFUND(Group.AUCTION, false),
    VENDOR_SALE(Group.VENDING, true),
    VENDOR_PURCHASE(Group.VENDING, false),
    PLAYTIME(Group.REWARD, false),
    DAILY(Group.REWARD, false),
    QUEST(Group.REWARD, false),
    LETTER(Group.OTHER, false),
    PLOT(Group.OTHER, false),
    WARP(Group.OTHER, false),
    TRANSFER(Group.OTHER, false),
    ADMIN(Group.OTHER, false);

    /** What the history's filter drop-down groups by. */
    public enum Group { AUCTION, VENDING, REWARD, OTHER }

    private final Group group;
    /** Counted by the "top earners" leaderboard: income from selling to other players. */
    private final boolean trade;

    LedgerReason(Group group, boolean trade) {
        this.group = group;
        this.trade = trade;
    }

    public Group group() {
        return group;
    }

    public boolean trade() {
        return trade;
    }

    public Component title() {
        return Component.translatable("gui.roll_mod.hub.history.reason." + name().toLowerCase(Locale.ROOT));
    }

    /** {@link #ADMIN} for a stored name this build does not know, rather than failing the row. */
    public static LedgerReason byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return ADMIN;
        }
    }
}
