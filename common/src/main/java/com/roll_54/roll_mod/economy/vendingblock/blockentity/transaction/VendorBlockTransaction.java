package com.roll_54.roll_mod.economy.vendingblock.blockentity.transaction;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository;
import com.roll_54.roll_mod.economy.vendingblock.Config;
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
    CompletableFuture<Boolean> paymentFuture;
    if (vendor.isDiscarding() || ownerId == null) {
      paymentFuture = CurrencyRepository.withdraw(serverBuyer.getUUID(), CurrencyType.MAIN, price);
    } else {
      paymentFuture = transfer(serverBuyer.getUUID(), ownerId, price);
    }

    paymentFuture
        .thenAcceptAsync(
            success -> {
              if (!success) {
                buyer.sendSystemMessage(Messages.playerInsufficientCurrency(price));
                vendor.checkErrorState();
                return;
              }

              giveProduct(buyer, vendor, position);

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
    CompletableFuture<Boolean> paymentFuture;
    if (vendor.isInfinite() || ownerId == null) {
      // Unlimited budget / no owner: print the payment to the seller.
      paymentFuture =
          CurrencyRepository.addBalance(serverSeller.getUUID(), CurrencyType.MAIN, price);
    } else {
      // Owner pays the seller from their balance.
      paymentFuture = transfer(ownerId, serverSeller.getUUID(), price);
    }

    paymentFuture
        .thenAcceptAsync(
            success -> {
              if (!success) {
                seller.sendSystemMessage(Messages.ownerCannotAfford());
                vendor.checkErrorState();
                return;
              }

              takeAndStore(seller, vendor, want, amount);

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

  public static CompletableFuture<Boolean> transfer(UUID from, UUID to, long amount) {
    if (amount <= 0 || from.equals(to)) {
      return CompletableFuture.completedFuture(false);
    }
    return CurrencyRepository.transfer(from, to, CurrencyType.MAIN, amount);
  }

  /** Deduct one purchase worth from storage (unless infinite) and give the items to the buyer. */
  private static void giveProduct(Player buyer, VendorBlockEntity vendor, Position position) {
    ItemStack product = position.item();
    int amount = position.amount();

    VendorBlockInventory.deductFromStorage(vendor, product, amount);

    int transfer = amount;
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
  }
}
