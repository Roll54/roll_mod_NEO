package com.roll_54.roll_mod.economy.vendingblock.auction;

import com.roll_54.roll_mod.economy.currency.ledger.LedgerReason;
import com.roll_54.roll_mod.economy.currency.repository.AuctionHistory;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyLedger;
import com.mojang.logging.LogUtils;
import com.roll_54.roll_mod.economy.api.CurrencyCause;
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
import javax.annotation.Nullable;
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
    track(player.getUUID(), player.getName().getString(), AuctionAction.LISTED, listing,
        bidding ? startPrice : fixedPrice, null, null);
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

    CurrencyCause purchase = CurrencyCause.player(buyer, LedgerReason.AUCTION_PURCHASE)
        .withSender(sellerId, listing.sellerName).withDetail("buy " + item(product) + " listing " + id);

    // Withdrawn and credited separately rather than transferred, so the seller can be paid less
    // than the buyer spent — the house's cut, see sellerPayout.
    withdraw(buyer.getUUID(), price, purchase)
        .thenAcceptAsync(
            success -> {
              AuctionListing current = data.findById(id);
              if (current == null) {
                if (success) // it sold to someone else — give the money back
                  CurrencyRepository.addBalance(buyer.getUUID(), CurrencyType.MAIN, price,
                      refund(purchase, "listing gone"));
                buyer.sendSystemMessage(Messages.ahNotFound());
                return;
              }
              if (!success) {
                buyer.sendSystemMessage(Messages.playerInsufficientCurrency(price));
                return;
              }

              creditSeller(server, sellerId, price, product, buyer.getUUID(), buyer.getName().getString(),
                  CurrencyCause.player(buyer, LedgerReason.AUCTION_SALE).withSender(buyer)
                      .withDetail("sale " + item(product)));
              CurrencyLedger.record(buyer.getUUID(), CurrencyType.MAIN, -price, LedgerReason.AUCTION_PURCHASE,
                  product, product.getCount(), sellerId, current.sellerName);
              data.removeListing(current);
              trackSale(buyer, current, product, price);
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

    CurrencyCause bid = CurrencyCause.player(bidder, LedgerReason.AUCTION_BID)
        .withSender(listing.sellerId, listing.sellerName)
        .withDetail("bid on " + item(listing.item) + " listing " + id);
    CurrencyRepository.withdraw(bidder.getUUID(), CurrencyType.MAIN, amount, bid)
        .thenAcceptAsync(
            success -> {
              AuctionListing current = data.findById(id);
              if (current == null) {
                if (success)
                  CurrencyRepository.addBalance(bidder.getUUID(), CurrencyType.MAIN, amount,
                      refund(bid, "listing gone"));
                bidder.sendSystemMessage(Messages.ahNotFound());
                return;
              }
              if (!success) {
                bidder.sendSystemMessage(Messages.playerInsufficientCurrency(amount));
                return;
              }

              long minNow = current.minNextBid(increment);
              if (amount < minNow) { // someone bid higher in the meantime — refund and reject
                CurrencyRepository.addBalance(bidder.getUUID(), CurrencyType.MAIN, amount,
                    refund(bid, "outbid meanwhile"));
                bidder.sendSystemMessage(Messages.ahBidTooLow(minNow));
                return;
              }

              // refund the previous highest bidder (release their escrow)
              if (current.currentBidderId != null && current.currentBid > 0) {
                trackRefund(
                    CurrencyRepository.addBalance(
                        current.currentBidderId, CurrencyType.MAIN, current.currentBid,
                        CurrencyCause.player(bidder, LedgerReason.AUCTION_REFUND).withSender(bidder)
                            .withDetail("escrow released: outbid on " + item(current.item))),
                    current, bidder);
                CurrencyLedger.record(current.currentBidderId, CurrencyType.MAIN, current.currentBid,
                    LedgerReason.AUCTION_REFUND, current.item, current.item.getCount(), null, null);
                ServerPlayer prev = server.getPlayerList().getPlayer(current.currentBidderId);
                if (prev != null) {
                  syncPlayerCurrency(prev);
                  prev.sendSystemMessage(Messages.ahOutbid(current.item.getHoverName()));
                }
              }

              CurrencyLedger.record(bidder.getUUID(), CurrencyType.MAIN, -amount, LedgerReason.AUCTION_BID,
                  current.item, current.item.getCount(), current.sellerId, current.sellerName);
              current.currentBid = amount;
              current.currentBidderId = bidder.getUUID();
              current.currentBidderName = bidder.getName().getString();
              data.setDirty();
              track(bidder.getUUID(), bidder.getName().getString(), AuctionAction.BID, current, amount,
                  current.sellerId, current.sellerName);
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

    CurrencyCause buyoutCause = CurrencyCause.player(buyer, LedgerReason.AUCTION_PURCHASE)
        .withSender(sellerId, listing.sellerName).withDetail("buyout " + item(product) + " listing " + id);
    CurrencyRepository.withdraw(buyer.getUUID(), CurrencyType.MAIN, price, buyoutCause)
        .thenAcceptAsync(
            success -> {
              AuctionListing current = data.findById(id);
              if (current == null) {
                if (success)
                  CurrencyRepository.addBalance(buyer.getUUID(), CurrencyType.MAIN, price,
                      refund(buyoutCause, "listing gone"));
                buyer.sendSystemMessage(Messages.ahNotFound());
                return;
              }
              if (!success) {
                buyer.sendSystemMessage(Messages.playerInsufficientCurrency(price));
                return;
              }

              if (current.currentBidderId != null && current.currentBid > 0) {
                trackRefund(
                    CurrencyRepository.addBalance(
                        current.currentBidderId, CurrencyType.MAIN, current.currentBid,
                        CurrencyCause.player(buyer, LedgerReason.AUCTION_REFUND).withSender(buyer)
                            .withDetail("escrow released: bought out " + item(current.item))),
                    current, buyer);
                CurrencyLedger.record(current.currentBidderId, CurrencyType.MAIN, current.currentBid,
                    LedgerReason.AUCTION_REFUND, current.item, current.item.getCount(), null, null);
                ServerPlayer prev = server.getPlayerList().getPlayer(current.currentBidderId);
                if (prev != null) {
                  syncPlayerCurrency(prev);
                  prev.sendSystemMessage(Messages.ahOutbid(current.item.getHoverName()));
                }
              }

              creditSeller(server, sellerId, price, product, buyer.getUUID(), buyer.getName().getString(),
                  CurrencyCause.player(buyer, LedgerReason.AUCTION_SALE).withSender(buyer)
                      .withDetail("sale " + item(product)));
              CurrencyLedger.record(buyer.getUUID(), CurrencyType.MAIN, -price, LedgerReason.AUCTION_PURCHASE,
                  product, product.getCount(), sellerId, current.sellerName);
              data.removeListing(current);
              trackSale(buyer, current, product, price);
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
    track(player.getUUID(), player.getName().getString(), AuctionAction.CANCELLED, listing,
        listing.displayPrice(), null, null);
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
      // add() shrinks the copy by what fit; what is gone from it is what was claimed.
      int claimed = stack.getCount() - copy.getCount();
      if (claimed > 0) {
        track(player.getUUID(), player.getName().getString(), AuctionAction.CLAIMED, null, stack,
            claimed, 0, null, null);
      }
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
        // The winner's money was taken (and logged) when they bid; only the seller is paid now.
        creditSeller(server, l.sellerId, l.currentBid, l.item, l.currentBidderId,
            l.currentBidderName == null ? "" : l.currentBidderName,
            CurrencyCause.system("auction-expiry", LedgerReason.AUCTION_SALE)
                .withSender(l.currentBidderId, l.currentBidderName)
                .withDetail("won auction " + item(l.item)));
        data.addClaim(l.currentBidderId, l.item.copy());
        String winner = l.currentBidderName == null ? "" : l.currentBidderName;
        track(l.currentBidderId, winner, AuctionAction.WON, l, l.currentBid, l.sellerId, l.sellerName);
        track(l.sellerId, l.sellerName, AuctionAction.SOLD, l, l.currentBid, l.currentBidderId, winner);
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
        track(l.sellerId, l.sellerName, AuctionAction.EXPIRED_RETURNED, l, l.displayPrice(), null, null);
        notify(
            server,
            l.sellerId,
            Messages.ahExpiredReturned(l.item.getCount(), l.item.getHoverName()));
      }
    }
    resyncAll(server);
  }

  /* ------------------------------------------------- history ----------------------------------------------------- */

  /**
   * Puts one action on record: a row of the player's {@link AuctionHistory} and a line of {@link
   * AuctionLog}. Called only once the action has gone through — the item moved, the money taken —
   * and never throws, so a history that cannot be written costs the trade nothing.
   *
   * @param listingId null for a claim, which belongs to no listing any more
   * @param count     what was acted on, which for a partial claim is less than the stack's count
   */
  private static void track(
      UUID player, String name, AuctionAction action, @Nullable UUID listingId,
      @Nullable ItemStack item, int count, long price, @Nullable UUID otherId,
      @Nullable String otherName) {
    AuctionHistory.record(player, action, listingId, item, count, price, otherId, otherName);
    AuctionLog.write(action, player, name, listingId, item, count, price, otherName);
  }

  /** {@link #track} for an action on a whole listing: its id, its item, its count. */
  private static void track(
      UUID player, String name, AuctionAction action, AuctionListing listing, long price,
      @Nullable UUID otherId, @Nullable String otherName) {
    track(player, name, action, listing.id, listing.item, listing.item.getCount(), price, otherId,
        otherName);
  }

  /** A fixed-price purchase or a buyout: BOUGHT for the buyer, SOLD for the seller, both at the full price. */
  private static void trackSale(ServerPlayer buyer, AuctionListing listing, ItemStack product, long price) {
    String buyerName = buyer.getName().getString();
    track(buyer.getUUID(), buyerName, AuctionAction.BOUGHT, listing.id, product, product.getCount(),
        price, listing.sellerId, listing.sellerName);
    track(listing.sellerId, listing.sellerName, AuctionAction.SOLD, listing.id, product,
        product.getCount(), price, buyer.getUUID(), buyerName);
  }

  /**
   * OUTBID_REFUND for {@code listing}'s current high bidder once {@code refund} — the escrow being
   * handed back — reports success. Their id, name and bid are read now, on the server thread,
   * because the caller overwrites them with the new bid before the database answers.
   */
  private static void trackRefund(
      CompletableFuture<Boolean> refund, AuctionListing listing, ServerPlayer by) {
    UUID previous = listing.currentBidderId;
    String previousName = listing.currentBidderName == null ? "" : listing.currentBidderName;
    long amount = listing.currentBid;
    UUID listingId = listing.id;
    ItemStack item = listing.item.copy();
    UUID byId = by.getUUID();
    String byName = by.getName().getString();
    refund.thenAccept(ok -> {
      if (Boolean.TRUE.equals(ok)) {
        track(previous, previousName, AuctionAction.OUTBID_REFUND, listingId, item, item.getCount(),
            amount, byId, byName);
      }
    });
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

  /**
   * What the seller receives for a sale at {@code price}: the price less the Auction House's cut
   * when {@code ahSellerFeeEnabled} is on. The fee itself is not validated (see Config), so the
   * result is only kept from going negative — an odd value must never turn a sale into a debit.
   */
  public static long sellerPayout(long price) {
    if (!Config.Server.AH_SELLER_FEE_ENABLED.get()) return price;
    double fee = Config.Server.AH_SELLER_FEE.get();
    return Math.max(0L, price - Math.round(price * fee));
  }

  /**
   * Pays the seller {@link #sellerPayout} of {@code price}, and — when that is less than the price —
   * tells them what was kept. The sold message itself still names the full price the buyer paid.
   */
  private static void creditSeller(
      MinecraftServer server, UUID sellerId, long price, ItemStack item, UUID buyerId, String buyerName,
      CurrencyCause cause) {
    long payout = sellerPayout(price);
    if (payout > 0) {
      CurrencyRepository.addBalance(sellerId, CurrencyType.MAIN, payout,
              payout == price ? cause : cause.withDetail(cause.detail() + ", after fee of " + (price - payout)))
          .thenAccept(
              ok -> {
                if (ok)
                  CurrencyLedger.record(sellerId, CurrencyType.MAIN, payout, LedgerReason.AUCTION_SALE,
                      item, item.getCount(), buyerId, buyerName);
              })
          .exceptionally(
              e -> {
                LOGGER.error("AH seller payout failed", e);
                return null;
              });
    }
    // What the house kept, as its own history line, so the seller can see where it went.
    CurrencyLedger.record(sellerId, CurrencyType.MAIN, payout - price, LedgerReason.AUCTION_FEE,
        item, item.getCount(), buyerId, buyerName);
    if (payout != price) {
      // Queued behind the sold message, which every caller sends right after this.
      server.execute(() -> notify(server, sellerId, Messages.ahSellerFee(payout, price - payout)));
    }
  }

  private static CompletableFuture<Boolean> withdraw(UUID from, long amount, CurrencyCause cause) {
    if (amount <= 0) return CompletableFuture.completedFuture(false);
    return CurrencyRepository.withdraw(from, CurrencyType.MAIN, amount, cause);
  }

  /** The same cause, as the money being handed back. */
  private static CurrencyCause refund(CurrencyCause original, String why) {
    return original.withReason(LedgerReason.AUCTION_REFUND)
        .withDetail("refund: " + why + " (" + original.detail() + ")");
  }

  /** {@code 3x minecraft:diamond}, for the audit log's detail. */
  private static String item(ItemStack stack) {
    return stack.getCount() + "x " + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
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
