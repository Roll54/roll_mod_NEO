package com.roll_54.roll_mod.economy.vendingblock.auction;

import com.mojang.logging.LogUtils;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository;
import com.roll_54.roll_mod.economy.vendingblock.Config;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.transaction.VendorBlockInventory;
import com.roll_54.roll_mod.economy.vendingblock.gui.chat.Messages;
import com.roll_54.roll_mod.economy.vendingblock.network.SyncAuctionsPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

/**
 * Server-side Auction House operations and viewer sync. All currency moves go through {@link
 * CurrencyRepository} (UUID-based, so offline-safe) and run on the server thread via {@code
 * thenAcceptAsync(..., server)} — mirroring {@code VendorBlockTransaction}. Items that can't be
 * delivered immediately fall into the recipient's persistent "collection" (claims).
 */
public final class AuctionManager {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Players with the AH UI currently open; they receive {@link SyncAuctionsPacket} on every change.
   */
  private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

  private AuctionManager() {}

  /* --------------------------------------------------- viewers --------------------------------------------------- */

  public static void addViewer(ServerPlayer player) {
    VIEWERS.add(player.getUUID());
    syncTo(player);
  }

  public static void removeViewer(UUID id) {
    VIEWERS.remove(id);
  }

  public static void syncTo(ServerPlayer player) {
    AuctionData data = AuctionData.get(player.server);
    PacketDistributor.sendToPlayer(
        player,
        new SyncAuctionsPacket(
            new ArrayList<>(data.getListings()),
            new ArrayList<>(data.getClaims(player.getUUID()))));
  }

  /** Pushes the listings, and each viewer's own claims, to everyone with the auction open. */
  public static void resyncAll(MinecraftServer server) {
    AuctionData data = AuctionData.get(server);
    List<AuctionListing> snapshot = new ArrayList<>(data.getListings());
    for (UUID id : VIEWERS) {
      ServerPlayer player = server.getPlayerList().getPlayer(id);
      if (player != null) {
        PacketDistributor.sendToPlayer(
            player, new SyncAuctionsPacket(snapshot, new ArrayList<>(data.getClaims(id))));
      }
    }
  }

  /* -------------------------------------------------- create ----------------------------------------------------- */

  public static void createListing(
      ServerPlayer player,
      ItemStack item,
      boolean bidding,
      long fixedPrice,
      long startPrice,
      long buyout,
      int days) {
    if (item == null || item.isEmpty()) return;
    MinecraftServer server = player.server;
    AuctionData data = AuctionData.get(server);

    int maxListings = LuckPermsCompat.maxListings(player);
    if (data.activeCountFor(player.getUUID()) >= maxListings) {
      player.sendSystemMessage(Messages.ahMaxListings(maxListings));
      return;
    }

    int maxDays = LuckPermsCompat.maxDurationDays(player);
    int clampedDays = Math.max(1, Math.min(days, maxDays));
    if (days > maxDays) player.sendSystemMessage(Messages.ahDurationCapped(maxDays));

    if (bidding) {
      if (startPrice <= 0) {
        player.sendSystemMessage(Messages.ahInvalidPrice());
        return;
      }
      if (buyout != 0 && buyout < startPrice) buyout = startPrice;
    } else {
      if (fixedPrice <= 0) {
        player.sendSystemMessage(Messages.ahInvalidPrice());
        return;
      }
    }

    int wanted = item.getCount();
    if (VendorBlockInventory.countInPlayer(player, item) < wanted) {
      player.sendSystemMessage(Messages.playerEmpty(item.getHoverName()));
      return;
    }
    int removed = VendorBlockInventory.removeFromPlayer(player, item, wanted);
    if (removed < wanted) {
      if (removed > 0) data.addClaim(player.getUUID(), item.copyWithCount(removed));
      player.sendSystemMessage(Messages.playerEmpty(item.getHoverName()));
      return;
    }

    long now = System.currentTimeMillis();
    long expires = now + (long) clampedDays * 86_400_000L;
    AuctionListing listing =
        new AuctionListing(
            UUID.randomUUID(),
            player.getUUID(),
            player.getName().getString(),
            item.copyWithCount(wanted),
            bidding,
            bidding ? 0L : fixedPrice,
            bidding ? startPrice : 0L,
            bidding ? buyout : 0L,
            now,
            expires);
    data.addListing(listing);
    player.sendSystemMessage(Messages.ahListed(wanted, item.getHoverName()));
    resyncAll(server);
  }

  /* --------------------------------------------------- buy ------------------------------------------------------- */

  public static void buyFixed(ServerPlayer buyer, UUID id) {
    MinecraftServer server = buyer.server;
    AuctionData data = AuctionData.get(server);
    AuctionListing listing = data.findById(id);
    if (listing == null || listing.bidding) {
      buyer.sendSystemMessage(Messages.ahNotFound());
      return;
    }
    if (listing.sellerId.equals(buyer.getUUID())) {
      buyer.sendSystemMessage(Messages.ahOwnListing());
      return;
    }

    long price = listing.fixedPrice;
    UUID sellerId = listing.sellerId;
    ItemStack product = listing.item.copy();

    pay(buyer.getUUID(), sellerId, price)
        .thenAcceptAsync(
            success -> {
              AuctionListing current = data.findById(id);
              if (current == null) {
                if (success)
                  pay(sellerId, buyer.getUUID(), price); // it sold to someone else — reverse
                buyer.sendSystemMessage(Messages.ahNotFound());
                return;
              }
              if (!success) {
                buyer.sendSystemMessage(Messages.playerInsufficientCurrency(price));
                return;
              }

              data.removeListing(current);
              giveOrClaim(buyer, data, product);
              buyer.sendSystemMessage(
                  Messages.ahBought(product.getCount(), product.getHoverName(), price));
              notify(
                  server,
                  sellerId,
                  Messages.ahSold(
                      product.getCount(),
                      product.getHoverName(),
                      buyer.getName().getString(),
                      price));
              syncPlayerCurrency(buyer);
              syncCurrency(server, sellerId);
              resyncAll(server);
            },
            server)
        .exceptionally(
            e -> {
              LOGGER.error("AH buy failed", e);
              buyer.sendSystemMessage(Messages.transactionFailed());
              return null;
            });
  }

  /* --------------------------------------------------- bid ------------------------------------------------------- */

  public static void placeBid(ServerPlayer bidder, UUID id, long amount) {
    MinecraftServer server = bidder.server;
    AuctionData data = AuctionData.get(server);
    AuctionListing listing = data.findById(id);
    if (listing == null || !listing.bidding) {
      bidder.sendSystemMessage(Messages.ahNotFound());
      return;
    }
    if (listing.sellerId.equals(bidder.getUUID())) {
      bidder.sendSystemMessage(Messages.ahOwnListing());
      return;
    }

    long increment = Config.Server.AH_MIN_BID_INCREMENT.get();
    long minNext = listing.minNextBid(increment);
    if (amount < minNext) {
      bidder.sendSystemMessage(Messages.ahBidTooLow(minNext));
      return;
    }

    CurrencyRepository.withdraw(bidder.getUUID(), CurrencyType.MAIN, amount)
        .thenAcceptAsync(
            success -> {
              AuctionListing current = data.findById(id);
              if (current == null) {
                if (success)
                  CurrencyRepository.addBalance(bidder.getUUID(), CurrencyType.MAIN, amount);
                bidder.sendSystemMessage(Messages.ahNotFound());
                return;
              }
              if (!success) {
                bidder.sendSystemMessage(Messages.playerInsufficientCurrency(amount));
                return;
              }

              long minNow = current.minNextBid(increment);
              if (amount < minNow) { // someone bid higher in the meantime — refund and reject
                CurrencyRepository.addBalance(bidder.getUUID(), CurrencyType.MAIN, amount);
                bidder.sendSystemMessage(Messages.ahBidTooLow(minNow));
                return;
              }

              // refund the previous highest bidder (release their escrow)
              if (current.currentBidderId != null && current.currentBid > 0) {
                CurrencyRepository.addBalance(
                    current.currentBidderId, CurrencyType.MAIN, current.currentBid);
                ServerPlayer prev = server.getPlayerList().getPlayer(current.currentBidderId);
                if (prev != null) {
                  syncPlayerCurrency(prev);
                  prev.sendSystemMessage(Messages.ahOutbid(current.item.getHoverName()));
                }
              }

              current.currentBid = amount;
              current.currentBidderId = bidder.getUUID();
              current.currentBidderName = bidder.getName().getString();
              data.setDirty();
              syncPlayerCurrency(bidder);
              bidder.sendSystemMessage(Messages.ahBidPlaced(current.item.getHoverName(), amount));
              resyncAll(server);
            },
            server)
        .exceptionally(
            e -> {
              LOGGER.error("AH bid failed", e);
              bidder.sendSystemMessage(Messages.transactionFailed());
              return null;
            });
  }

  /* ------------------------------------------------- buyout ------------------------------------------------------ */

  public static void buyout(ServerPlayer buyer, UUID id) {
    MinecraftServer server = buyer.server;
    AuctionData data = AuctionData.get(server);
    AuctionListing listing = data.findById(id);
    if (listing == null || !listing.bidding || listing.buyout <= 0) {
      buyer.sendSystemMessage(Messages.ahNotFound());
      return;
    }
    if (listing.sellerId.equals(buyer.getUUID())) {
      buyer.sendSystemMessage(Messages.ahOwnListing());
      return;
    }

    long price = listing.buyout;
    UUID sellerId = listing.sellerId;
    ItemStack product = listing.item.copy();

    CurrencyRepository.withdraw(buyer.getUUID(), CurrencyType.MAIN, price)
        .thenAcceptAsync(
            success -> {
              AuctionListing current = data.findById(id);
              if (current == null) {
                if (success)
                  CurrencyRepository.addBalance(buyer.getUUID(), CurrencyType.MAIN, price);
                buyer.sendSystemMessage(Messages.ahNotFound());
                return;
              }
              if (!success) {
                buyer.sendSystemMessage(Messages.playerInsufficientCurrency(price));
                return;
              }

              if (current.currentBidderId != null && current.currentBid > 0) {
                CurrencyRepository.addBalance(
                    current.currentBidderId, CurrencyType.MAIN, current.currentBid);
                ServerPlayer prev = server.getPlayerList().getPlayer(current.currentBidderId);
                if (prev != null) {
                  syncPlayerCurrency(prev);
                  prev.sendSystemMessage(Messages.ahOutbid(current.item.getHoverName()));
                }
              }

              CurrencyRepository.addBalance(sellerId, CurrencyType.MAIN, price);
              data.removeListing(current);
              giveOrClaim(buyer, data, product);
              buyer.sendSystemMessage(
                  Messages.ahBought(product.getCount(), product.getHoverName(), price));
              notify(
                  server,
                  sellerId,
                  Messages.ahSold(
                      product.getCount(),
                      product.getHoverName(),
                      buyer.getName().getString(),
                      price));
              syncPlayerCurrency(buyer);
              syncCurrency(server, sellerId);
              resyncAll(server);
            },
            server)
        .exceptionally(
            e -> {
              LOGGER.error("AH buyout failed", e);
              buyer.sendSystemMessage(Messages.transactionFailed());
              return null;
            });
  }

  /* ------------------------------------------------- cancel ------------------------------------------------------ */

  public static void cancel(ServerPlayer player, UUID id) {
    MinecraftServer server = player.server;
    AuctionData data = AuctionData.get(server);
    AuctionListing listing = data.findById(id);
    if (listing == null) {
      player.sendSystemMessage(Messages.ahNotFound());
      return;
    }
    if (!listing.sellerId.equals(player.getUUID())) {
      player.sendSystemMessage(Messages.ahNotOwner());
      return;
    }
    if (listing.bidding && listing.hasBids()) {
      player.sendSystemMessage(Messages.ahCannotCancel());
      return;
    }

    data.removeListing(listing);
    data.addClaim(player.getUUID(), listing.item.copy());
    player.sendSystemMessage(
        Messages.ahCancelled(listing.item.getCount(), listing.item.getHoverName()));
    resyncAll(server);
  }

  /* ------------------------------------------------- claim ------------------------------------------------------- */

  public static void claimAll(ServerPlayer player) {
    MinecraftServer server = player.server;
    AuctionData data = AuctionData.get(server);
    List<ItemStack> current = new ArrayList<>(data.getClaims(player.getUUID()));
    if (current.isEmpty()) {
      player.sendSystemMessage(Messages.ahNothingToClaim());
      return;
    }

    List<ItemStack> leftovers = new ArrayList<>();
    for (ItemStack stack : current) {
      ItemStack copy = stack.copy();
      player.getInventory().add(copy);
      if (!copy.isEmpty()) leftovers.add(copy);
    }
    data.setClaims(player.getUUID(), leftovers);
    player.sendSystemMessage(
        leftovers.isEmpty() ? Messages.ahClaimed() : Messages.ahClaimedPartial());
    resyncAll(server);
  }

  /* ------------------------------------------------- expiry ------------------------------------------------------ */

  public static void tickExpiry(MinecraftServer server) {
    AuctionData data = AuctionData.get(server);
    long now = System.currentTimeMillis();
    List<AuctionListing> expired = new ArrayList<>();
    for (AuctionListing l : data.getListings()) {
      if (l.isExpired(now)) expired.add(l);
    }
    if (expired.isEmpty()) return;

    for (AuctionListing l : expired) {
      data.removeListing(l);
      if (l.bidding && l.hasBids()) {
        CurrencyRepository.addBalance(l.sellerId, CurrencyType.MAIN, l.currentBid)
            .exceptionally(
                e -> {
                  LOGGER.error("AH expiry payout failed", e);
                  return false;
                });
        data.addClaim(l.currentBidderId, l.item.copy());
        String winner = l.currentBidderName == null ? "" : l.currentBidderName;
        notify(
            server,
            l.sellerId,
            Messages.ahSold(l.item.getCount(), l.item.getHoverName(), winner, l.currentBid));
        ServerPlayer winnerPlayer = server.getPlayerList().getPlayer(l.currentBidderId);
        if (winnerPlayer != null) {
          winnerPlayer.sendSystemMessage(
              Messages.ahWon(l.item.getCount(), l.item.getHoverName(), l.currentBid));
        }
        syncCurrency(server, l.sellerId);
      } else {
        data.addClaim(l.sellerId, l.item.copy());
        notify(
            server,
            l.sellerId,
            Messages.ahExpiredReturned(l.item.getCount(), l.item.getHoverName()));
      }
    }
    resyncAll(server);
  }

  /* ------------------------------------------------- helpers ----------------------------------------------------- */

  private static void giveOrClaim(ServerPlayer player, AuctionData data, ItemStack stack) {
    ItemStack copy = stack.copy();
    player.getInventory().add(copy);
    if (!copy.isEmpty()) {
      data.addClaim(player.getUUID(), copy);
      player.sendSystemMessage(Messages.ahToCollection());
    }
  }

  private static CompletableFuture<Boolean> pay(UUID from, UUID to, long amount) {
    if (amount <= 0 || from.equals(to)) return CompletableFuture.completedFuture(false);
    return CurrencyRepository.transfer(from, to, CurrencyType.MAIN, amount);
  }

  private static void notify(
      MinecraftServer server, UUID playerId, net.minecraft.network.chat.Component message) {
    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
    if (player != null) player.sendSystemMessage(message);
  }

  private static void syncCurrency(MinecraftServer server, UUID playerId) {
    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
    if (player != null) syncPlayerCurrency(player);
  }

  /**
   * Push the player's current balance to their client. {@code roll_mod} no longer exposes
   * a standalone sync handler; {@link CurrencyService#get} reads the balance and sends the sync
   * payload.
   */
  private static void syncPlayerCurrency(ServerPlayer player) {
    CurrencyService.get(player, CurrencyType.MAIN);
  }
}
