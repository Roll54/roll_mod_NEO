package com.roll_54.roll_mod.minestar.hub.gui;

import com.roll_54.roll_mod.economy.currency.ledger.HistoryFilter;
import com.roll_54.roll_mod.economy.currency.ledger.LedgerEntry;

import java.util.List;

/** The client's copy of the money history last asked for — see {@code HistoryPackets}. */
public final class ClientHistoryCache {

    public static volatile HistoryFilter FILTER = HistoryFilter.ALL;
    public static volatile List<LedgerEntry> ENTRIES = List.of();
    /** False until the first answer arrives, so the panel can say "loading" rather than "empty". */
    public static volatile boolean LOADED;
    public static volatile int VERSION;

    private ClientHistoryCache() {}
}
