package com.roll_54.roll_mod.economy.vendingblock.client;

import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionListing;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side snapshot of the Auction House, refreshed by {@code SyncAuctionsPacket}. The auction
 * UI ({@code AuctionUI}) reads from here when building on the client (there is no synced block
 * entity to pull from, unlike the vendor UIs).
 */
public final class ClientAuctionCache {

  public static List<AuctionListing> LISTINGS = new ArrayList<>();
  public static List<ItemStack> CLAIMS = new ArrayList<>();

  private ClientAuctionCache() {}

  public static void set(List<AuctionListing> listings, List<ItemStack> claims) {
    LISTINGS = listings;
    CLAIMS = claims;
  }
}
