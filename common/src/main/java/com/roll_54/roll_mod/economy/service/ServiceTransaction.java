package com.roll_54.roll_mod.economy.service;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.economy.api.CurrencyCause;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.ledger.LedgerReason;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyLedger;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository;
import com.roll_54.roll_mod.economy.vendingblock.Config;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.transaction.VendorBlockTransaction;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * One use of a service block, the way {@code VendorBlockTransaction} sells an item: work out how much
 * can move, take the payment, then — back on the server thread, where the world may have changed
 * during the database round trip — check again and either move it or refund.
 *
 * <p>The amount is "as much as fits": the whole item for charging and filling, the whole item's
 * contents for discharging and draining, or the requested XP. It is capped by the block's stock (or room,
 * for buy-back) unless the block is unlimited. The price is per batch ({@link ServiceBlockEntity#getBatch()}), rounded up.
 * A charging block in fixed-batch mode instead moves exactly one batch for exactly the price ({@link #cap}).
 */
public final class ServiceTransaction {

  /**
   * Bound on insert/extract calls into one item per use. Items take at most their max input or output
   * per call, so a slow one fills over several uses; the quote and the move run the same loop, so the
   * price always matches what moved.
   */
  private static final int MAX_STEPS = 1_000;

  private ServiceTransaction() {}

  /**
   * @param slot the customer's work slot (index 0) for energy and fluid services; null for XP
   * @param xpPoints for XP services, the points the customer asked for
   */
  public static void use(
      ServerPlayer player, ServiceBlockEntity be, @Nullable IItemHandlerModifiable slot, long xpPoints) {
    ServiceType type = be.getServiceType();
    if (type == null) {
      player.sendSystemMessage(ServiceMessages.notSetUp());
      return;
    }
    if (be.isOwner(player)) {
      player.sendSystemMessage(ServiceMessages.ownService());
      return;
    }

    long cap = cap(be);
    long amount = move(type, player, be, slot, xpPoints, cap, false);
    if (amount <= 0) {
      player.sendSystemMessage(ServiceMessages.nothing(type, be));
      return;
    }
    if (amount < cap) {
      player.sendSystemMessage(ServiceMessages.partialBatch(type, be));
      return;
    }
    long cost = cost(amount, be.getPrice(), be.getBatch());
    ItemStack work = slot == null ? ItemStack.EMPTY : slot.getStackInSlot(0).copy();

    UUID ownerId = be.getOwnerID();
    String ownerName = be.getOwnerUser() != null ? be.getOwnerUser() : "";
    String customerName = player.getName().getString();
    MinecraftServer server = player.server;

    if (cost == 0L) {
      move(type, player, be, slot, xpPoints, amount, true);
      player.sendSystemMessage(ServiceMessages.done(type, amount, 0L, ownerName));
      return;
    }

    // The customer pays for a sale; the owner pays for buy-back, or the server does for an unlimited
    // or ownerless block, as an infinite vendor prints its buy-mode payments.
    boolean sells = type.sells();
    boolean noOwnerSide = ownerId == null || (!sells && be.isUnlimited());
    LedgerReason customerReason = sells ? LedgerReason.SERVICE_PURCHASE : LedgerReason.SERVICE_SALE;
    CurrencyCause cause = CurrencyCause.player(player, customerReason)
        .withSender(ownerId, ownerName)
        .withDetail(type.id() + " " + amount + " at service block " + be.getBlockPos().toShortString()
            + (noOwnerSide ? (sells ? " (payment discarded)" : " (payment printed)") : ""));
    CompletableFuture<Boolean> payment;
    if (sells) {
      payment = noOwnerSide
          ? CurrencyRepository.withdraw(player.getUUID(), CurrencyType.MAIN, cost, cause)
          : VendorBlockTransaction.transfer(player.getUUID(), ownerId, cost, cause);
    } else {
      payment = noOwnerSide
          ? CurrencyRepository.addBalance(player.getUUID(), CurrencyType.MAIN, cost, cause)
          : VendorBlockTransaction.transfer(ownerId, player.getUUID(), cost, cause);
    }

    payment
        .thenAcceptAsync(
            success -> {
              if (!success) {
                player.sendSystemMessage(sells
                    ? ServiceMessages.insufficient(cost)
                    : ServiceMessages.ownerCannotAfford());
                return;
              }
              // Stock, room, the work item or the player's XP may all have changed meanwhile.
              if (player.hasDisconnected()
                  || be.isRemoved()
                  || move(type, player, be, slot, xpPoints, amount, false) < amount) {
                reverse(server, player, ownerId, cost, sells, noOwnerSide, cause);
                player.sendSystemMessage(ServiceMessages.changed());
                return;
              }
              move(type, player, be, slot, xpPoints, amount, true);

              CurrencyLedger.record(player.getUUID(), CurrencyType.MAIN, sells ? -cost : cost,
                  customerReason, work, work.isEmpty() ? 0 : 1, ownerId, ownerName);
              if (!noOwnerSide) {
                CurrencyLedger.record(ownerId, CurrencyType.MAIN, sells ? cost : -cost,
                    sells ? LedgerReason.SERVICE_SALE : LedgerReason.SERVICE_PURCHASE,
                    work, work.isEmpty() ? 0 : 1, player.getUUID(), customerName);
              }

              player.sendSystemMessage(ServiceMessages.done(type, amount, cost, ownerName));
              CurrencyService.get(player, CurrencyType.MAIN);
              if (!noOwnerSide) {
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
                if (owner != null) {
                  CurrencyService.get(owner, CurrencyType.MAIN);
                  // PURCHASE_MESSAGES is a CLIENT config, never loaded on a dedicated server.
                  if (!Config.Client.SPEC.isLoaded() || Config.Client.PURCHASE_MESSAGES.get()) {
                    owner.sendSystemMessage(ServiceMessages.ownerNotice(type, amount, cost, customerName));
                  }
                }
              }
            },
            server)
        .exceptionally(
            e -> {
              LOGGER.error("Service block transaction failed", e);
              player.sendSystemMessage(ServiceMessages.failed());
              return null;
            });
  }

  /**
   * The most one use may move: exactly one batch in fixed-batch mode, where anything less is refused
   * rather than sold at the full price; otherwise unbounded ("as much as fits"). At exactly one batch
   * {@link #cost} is exactly the price, so the price needs no separate rule.
   */
  public static long cap(ServiceBlockEntity be) {
    return be.isFixedBatch() ? be.getBatch() : Long.MAX_VALUE;
  }

  /** {@code ceil(amount * price / unit)}, without overflowing on large energy amounts. */
  public static long cost(long amount, long price, long unit) {
    if (amount <= 0 || price <= 0) return 0L;
    BigInteger[] qr = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(price))
        .divideAndRemainder(BigInteger.valueOf(Math.max(1L, unit)));
    BigInteger result = qr[1].signum() > 0 ? qr[0].add(BigInteger.ONE) : qr[0];
    return result.bitLength() < 63 ? result.longValue() : Long.MAX_VALUE;
  }

  /* ---------------------------------------------- moving ---------------------------------------------- */

  /**
   * Moves up to {@code cap} of the resource and answers how much moved. Everything happens on copies
   * — of the work item, and against the stock as numbers — and only {@code commit} writes the result
   * back into the slot, the block and the player. A dry run is the same code with {@code commit} off,
   * so the amount quoted is exactly the amount later moved — and the screens use it on the client to
   * preview the cost.
   */
  public static long move(ServiceType type, Player player, ServiceBlockEntity be,
                   @Nullable IItemHandlerModifiable slot, long xpPoints, long cap, boolean commit) {
    return switch (type.resource()) {
      case ENERGY -> moveEnergy(type, be, slot, cap, commit);
      case FLUID -> moveFluid(type, be, slot, cap, commit);
      case XP -> moveXp(type, player, be, Math.min(cap, xpPoints), commit);
    };
  }

  private static long moveEnergy(ServiceType type, ServiceBlockEntity be,
                                 @Nullable IItemHandlerModifiable slot, long cap, boolean commit) {
    if (slot == null || slot.getStackInSlot(0).isEmpty()) return 0L;
    ItemStack copy = slot.getStackInSlot(0).copy();
    ItemEnergy energy = ItemEnergy.of(copy);
    if (energy == null) return 0L;

    long limit = type.sells()
        ? (be.isUnlimited() ? cap : Math.min(cap, be.getEnergy()))
        : (be.isUnlimited() ? cap : Math.min(cap, be.getEnergyCapacity() - be.getEnergy()));
    // Items take at most their max input or output per call, so keep going until they stop.
    long moved = 0L;
    for (int step = 0; step < MAX_STEPS && moved < limit; step++) {
      long got = type.sells() ? energy.insert(limit - moved) : energy.extract(limit - moved);
      if (got <= 0) break;
      moved += got;
    }
    if (commit && moved > 0) {
      if (!be.isUnlimited()) be.addEnergy(type.sells() ? -moved : moved);
      slot.setStackInSlot(0, copy);
    }
    return moved;
  }

  private static long moveFluid(ServiceType type, ServiceBlockEntity be,
                                @Nullable IItemHandlerModifiable slot, long cap, boolean commit) {
    if (slot == null || slot.getStackInSlot(0).isEmpty()) return 0L;
    IFluidHandlerItem handler = slot.getStackInSlot(0).copyWithCount(1)
        .getCapability(Capabilities.FluidHandler.ITEM);
    if (handler == null) return 0L;
    int limit = (int) Math.min(cap, Integer.MAX_VALUE);
    var tank = be.getTank();

    int moved;
    if (type.sells()) {
      // An unlimited block still needs a little of the fluid in its tank to know what it sells.
      FluidStack held = tank.getFluid();
      if (held.isEmpty()) return 0L;
      int offer = be.isUnlimited() ? limit : Math.min(limit, held.getAmount());
      moved = handler.fill(held.copyWithAmount(offer), FluidAction.EXECUTE);
      if (commit && moved > 0 && !be.isUnlimited()) tank.drain(moved, FluidAction.EXECUTE);
    } else {
      FluidStack contained = handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
      if (contained.isEmpty()) return 0L;
      int want = Math.min(limit, contained.getAmount());
      int room = be.isUnlimited() ? want : tank.fill(contained.copyWithAmount(want), FluidAction.SIMULATE);
      if (room <= 0) return 0L;
      FluidStack drained = handler.drain(contained.copyWithAmount(room), FluidAction.EXECUTE);
      moved = drained.getAmount();
      if (commit && moved > 0 && !be.isUnlimited()) tank.fill(drained, FluidAction.EXECUTE);
    }
    if (commit && moved > 0) slot.setStackInSlot(0, handler.getContainer());
    return moved;
  }

  private static long moveXp(ServiceType type, Player player, ServiceBlockEntity be,
                             long requested, boolean commit) {
    if (requested <= 0) return 0L;
    boolean haveFluid = ServiceBlockEntity.xpFluid() != Fluids.EMPTY;
    if (!be.isUnlimited() && !haveFluid) return 0L;
    var tank = be.getTank();
    long playerPoints = ServiceXp.totalPoints(player);

    long moved;
    if (type.sells()) {
      moved = be.isUnlimited() ? requested : Math.min(requested, be.xpPoints());
      // Never past what an int total can hold.
      moved = Math.min(moved, Integer.MAX_VALUE - playerPoints);
      if (commit && moved > 0) {
        if (!be.isUnlimited()) tank.drain((int) (moved * ServiceType.MB_PER_XP_POINT), FluidAction.EXECUTE);
        ServiceXp.setTotalPoints((ServerPlayer) player, playerPoints + moved);
      }
    } else {
      long room = be.isUnlimited()
          ? requested
          : (tank.getCapacity() - tank.getFluidAmount()) / ServiceType.MB_PER_XP_POINT;
      moved = Math.min(Math.min(requested, playerPoints), room);
      if (commit && moved > 0) {
        if (!be.isUnlimited()) {
          tank.fill(new FluidStack(ServiceBlockEntity.xpFluid(), (int) (moved * ServiceType.MB_PER_XP_POINT)),
              FluidAction.EXECUTE);
        }
        ServiceXp.setTotalPoints((ServerPlayer) player, playerPoints - moved);
      }
    }
    return Math.max(0L, moved);
  }

  /* --------------------------------------------- refunds --------------------------------------------- */

  /** Undoes a payment after the service could no longer be given. */
  private static void reverse(MinecraftServer server, ServerPlayer player, @Nullable UUID ownerId, long cost,
                              boolean sells, boolean noOwnerSide, CurrencyCause payment) {
    CurrencyCause cause = payment.withDetail("reversal: service unavailable after payment (" + payment.detail() + ")");
    CompletableFuture<Boolean> undo;
    if (sells) {
      undo = noOwnerSide
          ? CurrencyRepository.addBalance(player.getUUID(), CurrencyType.MAIN, cost, cause)
          : CurrencyRepository.transfer(ownerId, player.getUUID(), CurrencyType.MAIN, cost, cause);
    } else {
      undo = noOwnerSide
          ? CurrencyRepository.withdraw(player.getUUID(), CurrencyType.MAIN, cost, cause)
          : CurrencyRepository.transfer(player.getUUID(), ownerId, CurrencyType.MAIN, cost, cause);
    }
    undo.thenAcceptAsync(
        ok -> {
          if (!ok) {
            LOGGER.error("Service block reversal of {} for {} failed (sells={}, owner={})",
                cost, player.getUUID(), sells, ownerId);
          } else {
            CurrencyService.get(player, CurrencyType.MAIN);
          }
        },
        server);
  }

  /* --------------------------------------------- items ---------------------------------------------- */

  /** An item's energy through GrandPower's long storage, or NeoForge's FE one when it has only that. */
  interface ItemEnergy {
    long insert(long max);

    long extract(long max);

    @Nullable
    static ItemEnergy of(ItemStack stack) {
      ILongEnergyStorage grand = stack.getCapability(ILongEnergyStorage.ITEM);
      if (grand != null) {
        return new ItemEnergy() {
          @Override
          public long insert(long max) {
            return grand.receive(max, false);
          }

          @Override
          public long extract(long max) {
            return grand.extract(max, false);
          }
        };
      }
      IEnergyStorage fe = stack.getCapability(Capabilities.EnergyStorage.ITEM);
      if (fe == null) return null;
      return new ItemEnergy() {
        @Override
        public long insert(long max) {
          return fe.receiveEnergy((int) Math.min(max, Integer.MAX_VALUE), false);
        }

        @Override
        public long extract(long max) {
          return fe.extractEnergy((int) Math.min(max, Integer.MAX_VALUE), false);
        }
      };
    }
  }
}
