package com.roll_54.roll_mod.economy.plot;

import com.roll_54.roll_mod.RollMod;
import com.mojang.logging.LogUtils;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.config.CurrencyConfig;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigManagerAPI;

/**
 * Server-side plot logic: pricing, per-viewer snapshots and the buy/resale/sell-back transactions.
 * Open Parties and Claims (OPAC) is the source of truth for ownership; {@link PlotListings} tracks
 * which owned plots are up for resale. Currency moves through {@link CurrencyService}/{@link
 * CurrencyRepository} (async, atomic withdraw) and all OPAC mutations run on the server thread via
 * {@code thenAcceptAsync(..., server)}, mirroring {@code AuctionManager}.
 *
 * <p>Pricing: a buyer always pays the escalated price {@link #priceFor} where the base is the
 * plot's starting price (server sale) or the seller's chosen price (resale). On a resale the seller
 * receives only the base; the escalation premium is burned. Selling back to the server returns the
 * plot's base starting price.
 */
public final class PlotService {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final ResourceLocation SHOP_DIM =
      RollMod.id("shop_dim");

  private static final int CLAIM_SUBCONFIG = 0;
  private static final boolean CLAIM_FORCELOAD = false;

  /** Per-player price escalation: every 10 plots owned multiplies a plot's price by this. */
  private static final double PRICE_ESCALATION_BASE = 1000.0;

  private PlotService() {}

  private static IServerClaimsManagerAPI claims(MinecraftServer server) {
    return OpenPACServerAPI.get(server).getServerClaimsManager();
  }

  private static boolean ownedBy(IServerClaimsManagerAPI cm, ChunkPos c, UUID who) {
    IPlayerChunkClaimAPI claim = cm.get(SHOP_DIM, c.x, c.z);
    return claim != null && who.equals(claim.getPlayerId());
  }

  /**
   * UUIDs of OPAC's special claim owners (server claim, expired claim) that protect the shop
   * dimension but do NOT count as a player owning a plot. A server-claimed plot is vacant/buyable.
   */
  private static Set<UUID> nonPlayerOwners(MinecraftServer server) {
    IPlayerConfigManagerAPI cfgs = OpenPACServerAPI.get(server).getPlayerConfigManager();
    return Set.of(
        cfgs.getServerClaimConfig().getPlayerId(), cfgs.getExpiredClaimConfig().getPlayerId());
  }

  /** OPAC's server claim owner: what protects an unsold plot. */
  private static UUID serverClaimOwner(MinecraftServer server) {
    return OpenPACServerAPI.get(server).getPlayerConfigManager().getServerClaimConfig().getPlayerId();
  }

  /** A plot is owned only if its anchor chunk is claimed by a real player (not server/expired). */
  private static boolean ownedByPlayerClaim(IPlayerChunkClaimAPI claim, Set<UUID> nonPlayers) {
    return claim != null && !nonPlayers.contains(claim.getPlayerId());
  }

  /** The real-player owner of a plot, or {@code null} if vacant (unclaimed or server-claimed). */
  private static UUID playerOwner(
      IServerClaimsManagerAPI cm, Set<UUID> nonPlayers, ChunkPos anchor) {
    IPlayerChunkClaimAPI claim = cm.get(SHOP_DIM, anchor.x, anchor.z);
    return ownedByPlayerClaim(claim, nonPlayers) ? claim.getPlayerId() : null;
  }

  /** Number of registered plots whose anchor chunk is claimed by {@code player}. */
  public static int ownedByPlayer(MinecraftServer server, UUID player) {
    IServerClaimsManagerAPI cm = claims(server);
    int n = 0;
    for (Plot p : PlotRegistry.all()) {
      if (ownedBy(cm, p.anchor(), player)) {
        n++;
      }
    }
    return n;
  }

  /**
   * Price of a plot for a buyer: the {@code base} price scaled by a per-player escalation of {@code
   * 1000^(owned/10)} ({@code owned} = plots they already own). At {@code owned == 0} the price is
   * exactly {@code base}; every 10 plots owned multiplies it by 1000.
   */
  public static long priceFor(long base, int owned) {
    return Math.round(base * Math.pow(PRICE_ESCALATION_BASE, owned / 10.0));
  }

  /**
   * The resale listing for a plot, but only if still valid (the listed seller still owns the plot).
   * Returns {@code null} for unlisted or stale entries.
   */
  private static PlotListings.Listing validListing(
      MinecraftServer server, IServerClaimsManagerAPI cm, Set<UUID> nonPlayers, Plot plot) {
    PlotListings.Listing listing = PlotListings.get(server).get(plot.id());
    if (listing == null) {
      return null;
    }
    return listing.seller().equals(playerOwner(cm, nonPlayers, plot.anchor())) ? listing : null;
  }

  /**
   * Per-viewer snapshot of every plot. {@code balance} is the viewer's current MAIN balance (read
   * once before this call) so affordability and price are baked in and the UI needs no async work.
   */
  public static List<PlotSnapshot> snapshot(ServerPlayer viewer, long balance) {
    MinecraftServer server = viewer.server;
    IServerClaimsManagerAPI cm = claims(server);
    Set<UUID> nonPlayers = nonPlayerOwners(server);
    UUID viewerId = viewer.getUUID();
    int ownedCount = ownedByPlayer(server, viewerId);

    List<PlotSnapshot> out = new ArrayList<>();
    for (Plot p : PlotRegistry.all()) {
      UUID owner = playerOwner(cm, nonPlayers, p.anchor());
      PlotSnapshot.Kind kind;
      String ownerName = null;
      long price = 0;
      boolean affordable = false;
      boolean listed = false;
      long listingPrice = 0;

      if (owner == null) {
        kind = PlotSnapshot.Kind.VACANT;
        price = priceFor(p.startingPrice(), ownedCount);
        affordable = balance >= price;
      } else if (owner.equals(viewerId)) {
        kind = PlotSnapshot.Kind.MINE;
        PlotListings.Listing listing = validListing(server, cm, nonPlayers, p);
        listed = listing != null;
        listingPrice = listed ? listing.basePrice() : 0;
      } else {
        ownerName = resolveName(server, cm, owner);
        PlotListings.Listing listing = validListing(server, cm, nonPlayers, p);
        if (listing != null) {
          kind = PlotSnapshot.Kind.OTHER_LISTED;
          price = priceFor(listing.basePrice(), ownedCount);
          affordable = balance >= price;
        } else {
          kind = PlotSnapshot.Kind.OTHER_UNLISTED;
        }
      }

      out.add(
          new PlotSnapshot(
              p.id(),
              p.pixelX(),
              p.pixelY(),
              p.pixelW(),
              p.pixelH(),
              kind,
              ownerName,
              price,
              affordable,
              p.startingPrice(),
              listed,
              listingPrice));
    }
    return out;
  }

  /* -------------------------------------------------- buy -------------------------------------------------------- */

  /**
   * Buy {@code plotId} — either a vacant plot from the server or another player's resale listing.
   * Validates the dimension is claimable, then dispatches. The buyer always pays the escalated
   * price; on a resale the seller receives the base and the premium is burned.
   */
  public static void purchase(ServerPlayer buyer, String plotId) {
    MinecraftServer server = buyer.server;
    Plot plot = PlotRegistry.byId(plotId);
    if (plot == null) {
      msg(buyer, "menu.roll_mod.plot.invalid");
      return;
    }
    IServerClaimsManagerAPI cm = claims(server);
    if (!cm.isClaimable(SHOP_DIM)) {
      msg(buyer, "menu.roll_mod.plot.notClaimable");
      return;
    }
    Set<UUID> nonPlayers = nonPlayerOwners(server);
    UUID owner = playerOwner(cm, nonPlayers, plot.anchor());
    if (owner == null) {
      buyFromServer(buyer, plot, cm, nonPlayers);
    } else if (owner.equals(buyer.getUUID())) {
      msg(buyer, "menu.roll_mod.plot.ownPlot");
    } else {
      buyResale(buyer, plot, cm, nonPlayers, owner);
    }
  }

  private static void buyFromServer(
      ServerPlayer buyer, Plot plot, IServerClaimsManagerAPI cm, Set<UUID> nonPlayers) {
    MinecraftServer server = buyer.server;
    String who = buyer.getGameProfile().getName();
    long price = priceFor(plot.startingPrice(), ownedByPlayer(server, buyer.getUUID()));
    logAction("{} attempting to buy {} for {}", who, plot.id(), price);

    CurrencyService.withdraw(buyer, CurrencyType.MAIN, price)
        .thenAcceptAsync(
            ok -> {
              if (!ok) {
                logAction("{} could not afford {} (price {})", who, plot.id(), price);
                msg(buyer, "menu.roll_mod.plot.insufficient", price);
                return;
              }
              if (playerOwner(cm, nonPlayers, plot.anchor()) != null) { // claimed during withdraw
                refund(buyer, price);
                msg(buyer, "menu.roll_mod.plot.takenRefunded");
                return;
              }
              if (!claimAll(cm, plot, buyer.getUUID())) {
                refund(buyer, price);
                msg(buyer, "menu.roll_mod.plot.claimFailed");
                return;
              }
              logAction("{} bought {} for {}", who, plot.id(), price);
              msg(buyer, "menu.roll_mod.plot.bought", price);
              PlotViewers.resync(server);
            },
            server)
        .exceptionally(e -> fail(buyer, price, "server buy", plot.id(), e));
  }

  private static void buyResale(
      ServerPlayer buyer,
      Plot plot,
      IServerClaimsManagerAPI cm,
      Set<UUID> nonPlayers,
      UUID seller) {
    MinecraftServer server = buyer.server;
    PlotListings.Listing listing = validListing(server, cm, nonPlayers, plot);
    if (listing == null) {
      msg(buyer, "menu.roll_mod.plot.notForSale");
      return;
    }
    String who = buyer.getGameProfile().getName();
    long base = listing.basePrice();
    long price = priceFor(base, ownedByPlayer(server, buyer.getUUID()));
    logAction(
        "{} attempting resale buy of {} for {} (seller gets {})", who, plot.id(), price, base);

    CurrencyService.withdraw(buyer, CurrencyType.MAIN, price)
        .thenAcceptAsync(
            ok -> {
              if (!ok) {
                msg(buyer, "menu.roll_mod.plot.insufficient", price);
                return;
              }
              // Re-validate: still owned by the same seller and listed at the same price.
              PlotListings.Listing now = validListing(server, cm, nonPlayers, plot);
              if (now == null || !now.seller().equals(seller) || now.basePrice() != base) {
                refund(buyer, price);
                msg(buyer, "menu.roll_mod.plot.takenRefunded");
                return;
              }
              if (!transfer(cm, plot, buyer.getUUID())) {
                refund(buyer, price);
                msg(buyer, "menu.roll_mod.plot.claimFailed");
                return;
              }
              PlotListings.get(server).remove(plot.id());
              // Seller receives only the base price; the escalation premium is burned.
              payout(server, seller, base);
              logAction(
                  "{} bought {} (resale) for {}; {} paid base {}",
                  who,
                  plot.id(),
                  price,
                  seller,
                  base);
              msg(buyer, "menu.roll_mod.plot.bought", price);
              notify(server, seller, "menu.roll_mod.plot.sold", base);
              PlotViewers.resync(server);
            },
            server)
        .exceptionally(e -> fail(buyer, price, "resale buy", plot.id(), e));
  }

  /* ------------------------------------------- list / unlist / sell ---------------------------------------------- */

  /** Put the viewer's plot up for resale at {@code basePrice} (the amount they will receive). */
  public static void listForResale(ServerPlayer owner, String plotId, long basePrice) {
    MinecraftServer server = owner.server;
    Plot plot = PlotRegistry.byId(plotId);
    if (plot == null) {
      msg(owner, "menu.roll_mod.plot.invalid");
      return;
    }
    if (!owner
        .getUUID()
        .equals(playerOwner(claims(server), nonPlayerOwners(server), plot.anchor()))) {
      msg(owner, "menu.roll_mod.plot.notOwner");
      return;
    }
    if (basePrice < 1) {
      msg(owner, "menu.roll_mod.plot.invalidPrice");
      return;
    }
    PlotListings.get(server).put(plotId, owner.getUUID(), basePrice);
    logAction("{} listed {} for resale at {}", owner.getGameProfile().getName(), plotId, basePrice);
    msg(owner, "menu.roll_mod.plot.listed", basePrice);
    PlotViewers.resync(server);
  }

  /** Remove the viewer's resale listing (the plot stays theirs). */
  public static void unlist(ServerPlayer owner, String plotId) {
    MinecraftServer server = owner.server;
    PlotListings listings = PlotListings.get(server);
    PlotListings.Listing listing = listings.get(plotId);
    if (listing == null || !listing.seller().equals(owner.getUUID())) {
      msg(owner, "menu.roll_mod.plot.notListed");
      return;
    }
    listings.remove(plotId);
    logAction("{} unlisted {}", owner.getGameProfile().getName(), plotId);
    msg(owner, "menu.roll_mod.plot.unlisted");
    PlotViewers.resync(server);
  }

  /**
   * Sell the viewer's plot back to the server for its base starting price. The plot goes back under
   * the OPAC server claim, the same protection a never-sold plot has.
   *
   * <p>Order matters, twice over. The chunks are handed back <em>before</em> the refund is paid:
   * this runs on the server thread, so a second sell request (a double click) arriving while the
   * deposit is in flight already sees the server as the owner and is refused — paying first let both
   * requests through, and paid twice. And they go to the server claim rather than being unclaimed:
   * a bare unclaim left the chunks as wild land that anyone could claim for free with OPAC's own
   * claim tool.
   */
  public static void sellToServer(ServerPlayer owner, String plotId) {
    MinecraftServer server = owner.server;
    Plot plot = PlotRegistry.byId(plotId);
    if (plot == null) {
      msg(owner, "menu.roll_mod.plot.invalid");
      return;
    }
    IServerClaimsManagerAPI cm = claims(server);
    UUID ownerId = owner.getUUID();
    if (!ownerId.equals(playerOwner(cm, nonPlayerOwners(server), plot.anchor()))) {
      msg(owner, "menu.roll_mod.plot.notOwner");
      return;
    }
    UUID serverClaim = serverClaimOwner(server);
    if (!transfer(cm, plot, serverClaim)) {
      // transfer may have got part-way; put the whole plot back in the seller's hands.
      transfer(cm, plot, ownerId);
      msg(owner, "menu.roll_mod.plot.failed");
      return;
    }
    PlotListings.get(server).remove(plotId);
    PlotViewers.resync(server);

    long refund = plot.startingPrice();
    String who = owner.getGameProfile().getName();
    CurrencyService.deposit(owner, CurrencyType.MAIN, refund, server)
        .thenAcceptAsync(
            ok -> {
              if (!ok) {
                restoreAfterFailedSale(server, cm, plot, ownerId, serverClaim, owner);
                return;
              }
              logAction("{} sold {} back to the server for {}", who, plotId, refund);
              msg(owner, "menu.roll_mod.plot.soldToServer", refund);
            },
            server)
        .exceptionally(
            e -> {
              LOGGER.error("Plot sell-to-server failed for {}", plotId, e);
              server.execute(
                  () -> restoreAfterFailedSale(server, cm, plot, ownerId, serverClaim, owner));
              return null;
            });
  }

  /**
   * The refund never arrived, so the sale is undone: the plot returns to the seller. Only if it is
   * still the server's, though — while the deposit was in flight someone may have bought it, and
   * that buyer paid for it.
   */
  private static void restoreAfterFailedSale(
      MinecraftServer server,
      IServerClaimsManagerAPI cm,
      Plot plot,
      UUID sellerId,
      UUID serverClaim,
      ServerPlayer seller) {
    if (ownedBy(cm, plot.anchor(), serverClaim) && transfer(cm, plot, sellerId)) {
      LOGGER.warn("Plot sell-to-server refund failed for {}; returned it to {}", plot.id(), sellerId);
      PlotViewers.resync(server);
    } else {
      LOGGER.error(
          "Plot sell-to-server refund failed for {} and the plot is no longer the server's;"
              + " {} was not refunded {}",
          plot.id(),
          sellerId,
          plot.startingPrice());
    }
    msg(seller, "menu.roll_mod.plot.failed");
  }

  /* ---------------------------------------------- protection repair ---------------------------------------------- */

  /**
   * Puts every chunk of every unsold plot back under the OPAC server claim. Plots used to be sold
   * back by plain unclaiming, which left them as wild land anyone could claim for free; this closes
   * those holes in existing worlds, and anything that slipped under a player's own claim tool on an
   * unsold plot is taken back with them.
   *
   * <p>An owned plot is only reported, never changed: a chunk of it held by someone else or by no
   * one may be the same leftover, but taking land out from under a paying owner is an admin's call.
   */
  public static void protectVacantPlots(MinecraftServer server) {
    IServerClaimsManagerAPI cm = claims(server);
    Set<UUID> nonPlayers = nonPlayerOwners(server);
    UUID serverClaim = serverClaimOwner(server);

    int reclaimed = 0;
    for (Plot plot : PlotRegistry.all()) {
      UUID owner = playerOwner(cm, nonPlayers, plot.anchor());
      for (ChunkPos c : plot.chunks()) {
        IPlayerChunkClaimAPI claim = cm.get(SHOP_DIM, c.x, c.z);
        UUID holder = claim == null ? null : claim.getPlayerId();
        if (owner == null) {
          if (!serverClaim.equals(holder)) {
            cm.unclaim(SHOP_DIM, c.x, c.z);
            cm.claim(SHOP_DIM, serverClaim, CLAIM_SUBCONFIG, c.x, c.z, CLAIM_FORCELOAD);
            reclaimed++;
          }
        } else if (!owner.equals(holder)) {
          LOGGER.warn(
              "[plot] {} is owned by {} but chunk {} is held by {}",
              plot.id(),
              owner,
              c,
              holder == null ? "nobody" : holder);
        }
      }
    }
    if (reclaimed > 0) {
      LOGGER.info("[plot] Put {} unprotected chunk(s) of unsold plots back under the server claim.", reclaimed);
      PlotViewers.resync(server);
    }
  }

  /* ------------------------------------------------- helpers ----------------------------------------------------- */

  // OPAC signature is claim(dim, owner, subConfigIndex, x, z, forceload).
  private static boolean claimAll(IServerClaimsManagerAPI cm, Plot plot, UUID who) {
    try {
      for (ChunkPos c : plot.chunks()) {
        cm.claim(SHOP_DIM, who, CLAIM_SUBCONFIG, c.x, c.z, CLAIM_FORCELOAD);
      }
      return true;
    } catch (Exception ex) {
      LOGGER.error("Plot claim failed for {}", plot.id(), ex);
      return false;
    }
  }

  private static boolean transfer(IServerClaimsManagerAPI cm, Plot plot, UUID buyer) {
    try {
      for (ChunkPos c : plot.chunks()) {
        cm.unclaim(SHOP_DIM, c.x, c.z);
        cm.claim(SHOP_DIM, buyer, CLAIM_SUBCONFIG, c.x, c.z, CLAIM_FORCELOAD);
      }
      return true;
    } catch (Exception ex) {
      LOGGER.error("Plot transfer failed for {}", plot.id(), ex);
      return false;
    }
  }

  private static void refund(ServerPlayer buyer, long price) {
    CurrencyService.deposit(buyer, CurrencyType.MAIN, price, buyer.server);
  }

  /** Pay an arbitrary (possibly offline) player; sync if they are online. */
  private static void payout(MinecraftServer server, UUID playerId, long amount) {
    CurrencyRepository.addBalance(playerId, CurrencyType.MAIN, amount)
        .thenAcceptAsync(
            ok -> {
              ServerPlayer online = server.getPlayerList().getPlayer(playerId);
              if (online != null) {
                CurrencyService.get(online, CurrencyType.MAIN);
              }
            },
            server)
        .exceptionally(
            e -> {
              LOGGER.error("Plot resale payout failed for {}", playerId, e);
              return null;
            });
  }

  private static Void fail(
      ServerPlayer buyer, long price, String what, String plotId, Throwable e) {
    LOGGER.error("Plot {} failed for {}", what, plotId, e);
    refund(buyer, price);
    msg(buyer, "menu.roll_mod.plot.failed");
    return null;
  }

  private static String resolveName(MinecraftServer server, IServerClaimsManagerAPI cm, UUID id) {
    ServerPlayer online = server.getPlayerList().getPlayer(id);
    if (online != null) {
      return online.getGameProfile().getName();
    }
    // OPAC tracks the last-seen username, so this works for offline owners too.
    IServerPlayerClaimInfoAPI info = cm.getPlayerInfo(id);
    if (info != null) {
      String name = info.getPlayerUsername();
      if (name != null && !name.isBlank()) {
        return name;
      }
    }
    return id.toString().substring(0, 8);
  }

  private static void notify(MinecraftServer server, UUID playerId, String key, Object... args) {
    ServerPlayer online = server.getPlayerList().getPlayer(playerId);
    if (online != null) {
      online.sendSystemMessage(Component.translatable(key, args));
    }
  }

  private static void msg(ServerPlayer player, String key, Object... args) {
    player.sendSystemMessage(Component.translatable(key, args));
  }

  /** Log a plot action to the server log when {@code plot.logActions} is enabled in the config. */
  private static void logAction(String format, Object... args) {
    CurrencyConfig cfg = CurrencyConfig.MAIN;
    if (cfg != null && cfg.plot.logActions) {
      LOGGER.info("[plot] " + format, args);
    }
  }
}
