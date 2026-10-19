package com.roll_54.roll_mod.economy.vendingblock.blockentity.transaction;

import com.roll_54.roll_mod.economy.currency.ledger.LedgerReason;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyLedger;
import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.economy.api.CurrencyCause;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository;
import com.roll_54.roll_mod.economy.minprice.MinPriceService;
import com.roll_54.roll_mod.economy.vendingblock.Config;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.Position;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.chat.Messages;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class VendorBlockTransaction {

  public static void purchase(
      Level level, Player buyer, VendorBlockEntity vendor, int positionIndex) {
    if (!(buyer instanceof ServerPlayer serverBuyer)) return;

    Position position = vendor.getPosition(positionIndex);
    if (position == null || position.item().isEmpty()) {
      buyer.sendSystemMessage(Messages.vendorEmpty());
      return;
    }

    ItemStack product = position.displayStack(); // item with count = amount per purchase
    long price = position.price();

    if (belowFloor(serverBuyer.server, vendor, position)) {
      buyer.sendSystemMessage(Messages.minPriceOfferBlocked());
      return;
    }

    UUID ownerId = vendor.getOwnerID();
    // Owner username can be unresolved (null); Component.translatable rejects null args.
    String ownerName = vendor.getOwnerUser() != null ? vendor.getOwnerUser() : "";
    String buyerName = buyer.getName().getString();

    if (vendor.purchasesLeft(position) < 1) {
      buyer.sendSystemMessage(Messages.vendorSold());
      vendor.checkErrorState();
      return;
    }

    if (!VendorBlockInventory.checkInventorySpace(buyer, product)) {
      buyer.sendSystemMessage(Messages.playerFull());
      vendor.checkErrorState();
      return;
    }

    MinecraftServer server = serverBuyer.server;

    /* ================= FREE ================= */
    if (price == 0L) {
      giveProduct(buyer, vendor, position);
      buyer.sendSystemMessage(
          Messages.playerGiveaway(product.getCount(), product.getHoverName(), ownerName));
      vendor.checkErrorState();
      return;
    }

    /* ================= CURRENCY ================= */
    // Capture the payment mode now: the reversal below must undo exactly what was done,
    // even if an admin flips the vendor's flags during the DB roundtrip.
    boolean voidPayment = vendor.isDiscarding() || ownerId == null;
    CurrencyCause cause = CurrencyCause.player(serverBuyer, LedgerReason.VENDOR_PURCHASE)
        .withSender(ownerId, ownerName)
        .withDetail("buy " + item(product) + " at vendor " + vendor.getBlockPos().toShortString()
            + (voidPayment ? " (payment discarded)" : ""));
    CompletableFuture<Boolean> paymentFuture;
    if (voidPayment) {
      paymentFuture = CurrencyRepository.withdraw(serverBuyer.getUUID(), CurrencyType.MAIN, price, cause);
    } else {
      paymentFuture = transfer(serverBuyer.getUUID(), ownerId, price, cause);
    }

    paymentFuture
        .thenAcceptAsync(
            success -> {
              if (!success) {
                buyer.sendSystemMessage(Messages.playerInsufficientCurrency(price));
                vendor.checkErrorState();
                return;
              }

              // The world may have changed during the async payment roundtrip: the stock
              // was only checked before it, the block may have been broken (dropping its
              // storage), or the buyer may have logged out. Without this re-check two
              // racing buyers of the last stock would both be handed items.
              if (serverBuyer.hasDisconnected()
                  || vendor.isRemoved()
                  || vendor.purchasesLeft(position) < 1) {
                refundPurchase(server, serverBuyer, ownerId, price, voidPayment, cause);
                buyer.sendSystemMessage(Messages.vendorSold());
                vendor.checkErrorState();
                return;
              }

              giveProduct(buyer, vendor, position);

              // History: the buyer paid; the owner was paid unless the payment went nowhere.
              CurrencyLedger.record(serverBuyer.getUUID(), CurrencyType.MAIN, -price,
                  LedgerReason.VENDOR_PURCHASE, product, product.getCount(), ownerId, ownerName);
              if (!voidPayment) {
                CurrencyLedger.record(ownerId, CurrencyType.MAIN, price, LedgerReason.VENDOR_SALE,
                    product, product.getCount(), serverBuyer.getUUID(), buyerName);
              }

              buyer.sendSystemMessage(
                  Messages.playerBoughtCurrency(
                      product.getCount(), product.getHoverName(), ownerName, price));

              syncPlayerCurrency(serverBuyer);

              if (ownerId != null && !vendor.isDiscarding()) {
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
                if (owner != null) {
                  syncPlayerCurrency(owner);
                  // PURCHASE_MESSAGES is a CLIENT config; on a dedicated server it is never
                  // loaded, so default to sending when it's unavailable.
                  if (!Config.Client.SPEC.isLoaded() || Config.Client.PURCHASE_MESSAGES.get()) {
                    owner.sendSystemMessage(
                        Messages.ownerSoldCurrency(
                            product.getCount(), product.getHoverName(), buyerName, price));
                  }
                }
              }

              vendor.checkErrorState();
            },
            server)
        .exceptionally(
            e -> {
              LOGGER.error("Vendor purchase failed", e);
              buyer.sendSystemMessage(Messages.transactionFailed());
              vendor.checkErrorState();
              return null;
            });
  }

  /**
   * Whether a position has fallen under the minimal price since it was set (a {@code /reload}, a
   * config edit). It is refused, not removed: the owner sees it and fixes the price. Infinite
   * vendors are the server's own, and an owner online with the bypass keeps theirs.
   */
  private static boolean belowFloor(MinecraftServer server, VendorBlockEntity vendor, Position position) {
    if (!MinPriceService.enabled() || vendor.isInfinite()) return false;
    UUID ownerId = vendor.getOwnerID();
    ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
    if (owner != null && LuckPermsCompat.canBypassMinPrice(owner)) return false;
    return MinPriceService.checkPrice(position.item(), position.amount(), position.price()).isPresent();
  }

  /**
   * Buy-mode counterpart of {@link #purchase}: the player ({@code seller}) hands items to the
   * vendor and is paid. Payment comes from the owner's balance (or is printed when the vendor is
   * infinite / ownerless); bought items go into storage unless the vendor is discarding them.
   */
  public static void sell(Level level, Player seller, VendorBlockEntity vendor, int positionIndex) {
    if (!(seller instanceof ServerPlayer serverSeller)) return;

    Position position = vendor.getPosition(positionIndex);
    if (position == null || position.item().isEmpty()) {
      seller.sendSystemMessage(Messages.vendorEmpty());
      return;
    }

    ItemStack want = position.item();
    int amount = position.amount();
    long price = position.price();

    if (belowFloor(serverSeller.server, vendor, position)) {
      seller.sendSystemMessage(Messages.minPriceOfferBlocked());
      return;
    }

    UUID ownerId = vendor.getOwnerID();
    String ownerName = vendor.getOwnerUser() != null ? vendor.getOwnerUser() : "";
    String sellerName = seller.getName().getString();

    if (VendorBlockInventory.countInPlayer(seller, want) < amount) {
      seller.sendSystemMessage(Messages.playerEmpty(want.getHoverName()));
      return;
    }

    if (!vendor.isDiscarding() && !vendor.canStore(want, amount)) {
      seller.sendSystemMessage(Messages.vendorFull());
      vendor.checkErrorState();
      return;
    }

    MinecraftServer server = serverSeller.server;

    /* ================= FREE (donation) ================= */
    if (price == 0L) {
      takeAndStore(seller, vendor, want, amount);
      seller.sendSystemMessage(Messages.playerRequest(amount, want.getHoverName(), ownerName));
      notifyOwnerReceived(server, vendor, ownerId, amount, want.getHoverName(), sellerName, 0L);
      vendor.checkErrorState();
      return;
    }

    /* ================= CURRENCY ================= */
    // Capture the payment mode now so the reversal below undoes exactly what was done.
    boolean printedPayment = vendor.isInfinite() || ownerId == null;
    CurrencyCause cause = CurrencyCause.player(serverSeller, LedgerReason.VENDOR_SALE)
        .withSender(ownerId, ownerName)
        .withDetail("sell " + amount + "x " + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(want.getItem())
            + " at vendor " + vendor.getBlockPos().toShortString()
            + (printedPayment ? " (payment printed)" : ""));
    CompletableFuture<Boolean> paymentFuture;
    if (printedPayment) {
      // Unlimited budget / no owner: print the payment to the seller.
      paymentFuture =
          CurrencyRepository.addBalance(serverSeller.getUUID(), CurrencyType.MAIN, price, cause);
    } else {
      // Owner pays the seller from their balance.
      paymentFuture = transfer(ownerId, serverSeller.getUUID(), price, cause);
    }

    paymentFuture
        .thenAcceptAsync(
            success -> {
              if (!success) {
                seller.sendSystemMessage(Messages.ownerCannotAfford());
                vendor.checkErrorState();
                return;
              }

              // The items were only counted before the async payment: the seller may have
              // dropped them, logged out, or spam-clicked several sells backed by one batch.
              // Re-check on the server thread (nothing can change between this check and
              // takeAndStore below) and reverse the payment if the goods are gone.
              if (serverSeller.hasDisconnected()
                  || vendor.isRemoved()
                  || VendorBlockInventory.countInPlayer(seller, want) < amount) {
                reverseSellPayment(server, serverSeller, ownerId, price, printedPayment, cause);
                seller.sendSystemMessage(Messages.playerEmpty(want.getHoverName()));
                vendor.checkErrorState();
                return;
              }

              takeAndStore(seller, vendor, want, amount);

              // History, buy mode: the player sold to the shop; its owner paid unless the
              // payment was printed.
              CurrencyLedger.record(serverSeller.getUUID(), CurrencyType.MAIN, price,
                  LedgerReason.VENDOR_SALE, want, amount, ownerId, ownerName);
              if (!printedPayment) {
                CurrencyLedger.record(ownerId, CurrencyType.MAIN, -price, LedgerReason.VENDOR_PURCHASE,
                    want, amount, serverSeller.getUUID(), sellerName);
              }

              seller.sendSystemMessage(
                  Messages.playerSoldCurrency(amount, want.getHoverName(), ownerName, price));

              syncPlayerCurrency(serverSeller);
              notifyOwnerReceived(
                  server, vendor, ownerId, amount, want.getHoverName(), sellerName, price);

              vendor.checkErrorState();
            },
            server)
        .exceptionally(
            e -> {
              LOGGER.error("Vendor sell failed", e);
              seller.sendSystemMessage(Messages.transactionFailed());
              vendor.checkErrorState();
              return null;
            });
  }

  /**
   * Take {@code amount} of {@code want} from the seller and store it (unless the vendor discards).
   */
  private static void takeAndStore(
      Player seller, VendorBlockEntity vendor, ItemStack want, int amount) {
    int removed = VendorBlockInventory.removeFromPlayer(seller, want, amount);
    if (vendor.isDiscarding()) return;
    int remaining = removed;
    for (int slot = 0; slot < vendor.storage.getSlots() && remaining > 0; slot++) {
      ItemStack leftover = vendor.storage.insertItem(slot, want.copyWithCount(remaining), false);
      remaining = leftover.isEmpty() ? 0 : leftover.getCount();
    }
  }

  /** Pay the owner the courtesy notification (and sync their balance if they were paid). */
  private static void notifyOwnerReceived(
      MinecraftServer server,
      VendorBlockEntity vendor,
      UUID ownerId,
      int amount,
      net.minecraft.network.chat.Component item,
      String sellerName,
      long price) {
    if (ownerId == null || vendor.isInfinite()) return;
    ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
    if (owner == null) return;
    if (price > 0L) syncPlayerCurrency(owner);
    if (!Config.Client.SPEC.isLoaded() || Config.Client.PURCHASE_MESSAGES.get()) {
      owner.sendSystemMessage(
          price > 0L
              ? Messages.ownerBoughtCurrency(amount, item, sellerName, price)
              : Messages.ownerRequest(amount, item, sellerName));
    }
  }

  /**
   * Push the player's current balance to their client. {@code roll_mod} no longer exposes
   * a standalone sync handler; {@link CurrencyService#get} reads the balance and sends the sync
   * payload.
   */
  private static void syncPlayerCurrency(ServerPlayer player) {
    CurrencyService.get(player, CurrencyType.MAIN);
  }

  public static CompletableFuture<Boolean> transfer(UUID from, UUID to, long amount, CurrencyCause cause) {
    if (amount <= 0 || from.equals(to)) {
      return CompletableFuture.completedFuture(false);
    }
    return CurrencyRepository.transfer(from, to, CurrencyType.MAIN, amount, cause);
  }

  /** {@code 3x minecraft:diamond}, for the audit log's detail. */
  private static String item(ItemStack stack) {
    return stack.getCount() + "x " + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
  }

  /** The payment's own cause, as its reversal. */
  private static CurrencyCause reversal(CurrencyCause payment, String why) {
    return payment.withDetail("reversal: " + why + " (" + payment.detail() + ")");
  }

  /** Deduct one purchase worth from storage (unless infinite) and give the items to the buyer. */
  private static void giveProduct(Player buyer, VendorBlockEntity vendor, Position position) {
    ItemStack product = position.item();
    int amount = position.amount();

    // Only hand out what actually came out of storage — a race that drained the stock
    // during the payment must not conjure items.
    int transfer = VendorBlockInventory.deductFromStorage(vendor, product, amount);
    for (int i = 0; i < 36 && transfer > 0; i++) {
      ItemStack slot = buyer.getInventory().getItem(i);
      if (slot.isEmpty()) {
        int space = Math.min(transfer, product.getMaxStackSize());
        buyer.getInventory().setItem(i, product.copyWithCount(space));
        transfer -= space;
      } else if (ItemStack.isSameItemSameComponents(slot, product)) {
        int space = Math.min(slot.getMaxStackSize() - slot.getCount(), transfer);
        if (space > 0) {
          slot.grow(space);
          transfer -= space;
        }
      }
    }
    // Inventory filled up during the payment roundtrip: drop the paid-for rest instead of
    // silently destroying it.
    if (transfer > 0) {
      buyer.drop(product.copyWithCount(transfer), false);
    }
  }

  /** Undo a purchase payment after the goods turned out to be unavailable. */
  private static void refundPurchase(
      MinecraftServer server, ServerPlayer buyer, UUID ownerId, long price, boolean voidPayment,
      CurrencyCause payment) {
    CurrencyCause cause = reversal(payment, "goods unavailable after payment");
    CompletableFuture<Boolean> refund =
        voidPayment
            ? CurrencyRepository.addBalance(buyer.getUUID(), CurrencyType.MAIN, price, cause)
            : CurrencyRepository.transfer(ownerId, buyer.getUUID(), CurrencyType.MAIN, price, cause);
    refund.thenAcceptAsync(
        ok -> {
          if (!ok) {
            LOGGER.error(
                "Vendor purchase refund of {} to {} failed (voidPayment={}, owner={})",
                price,
                buyer.getUUID(),
                voidPayment,
                ownerId);
          } else {
            syncPlayerCurrency(buyer);
          }
        },
        server);
  }

  /** Undo a sell payment after the seller's goods turned out to be unavailable. */
  private static void reverseSellPayment(
      MinecraftServer server,
      ServerPlayer seller,
      UUID ownerId,
      long price,
      boolean printedPayment,
      CurrencyCause payment) {
    CurrencyCause cause = reversal(payment, "seller's goods gone after payment");
    CompletableFuture<Boolean> reverse =
        printedPayment
            ? CurrencyRepository.withdraw(seller.getUUID(), CurrencyType.MAIN, price, cause)
            : CurrencyRepository.transfer(seller.getUUID(), ownerId, CurrencyType.MAIN, price, cause);
    reverse.thenAcceptAsync(
        ok -> {
          if (!ok) {
            LOGGER.error(
                "Vendor sell reversal of {} from {} failed (printedPayment={}, owner={})",
                price,
                seller.getUUID(),
                printedPayment,
                ownerId);
          } else {
            syncPlayerCurrency(seller);
          }
        },
        server);
  }
}
