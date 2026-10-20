package com.roll_54.roll_mod.economy.vendingblock.client;

import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionHistoryEntry;
import java.util.List;

/**
 * The client's copy of its own auction history, as last asked for — see {@code
 * AuctionHistoryPackets}. The auction UI's History panel rebuilds whenever {@link #VERSION} moves.
 */
public final class ClientAuctionHistoryCache {

  public static volatile List<AuctionHistoryEntry> ENTRIES = List.of();
  /** False until the first answer arrives, so the panel can say "loading" rather than "empty". */
  public static volatile boolean LOADED;
  public static volatile int VERSION;

  private ClientAuctionHistoryCache() {}
}
