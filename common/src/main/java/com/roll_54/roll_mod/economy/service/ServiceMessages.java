package com.roll_54.roll_mod.economy.service;

import net.minecraft.network.chat.Component;

/** Chat lines of the service block, like the vendor's {@code Messages}. */
public final class ServiceMessages {

  private ServiceMessages() {}

  public static Component notSetUp() {
    return Component.translatable("msg.roll_mod.service.not_set_up");
  }

  public static Component ownService() {
    return Component.translatable("msg.roll_mod.service.own");
  }

  /** Nothing could move: out of stock, full, or nothing in (or room in) the customer's item. */
  public static Component nothing(ServiceType type, ServiceBlockEntity be) {
    if (!be.isUnlimited()) {
      if (type.sells() && stock(type, be) <= 0) return Component.translatable("msg.roll_mod.service.out_of_stock");
      if (!type.sells() && room(type, be) <= 0) return Component.translatable("msg.roll_mod.service.full");
    }
    return Component.translatable("msg.roll_mod.service.nothing." + type.resource().name().toLowerCase(java.util.Locale.ROOT));
  }

  /** Fixed-batch mode, and less than a whole batch could move: out of stock for one, or the item is nearly full. */
  public static Component partialBatch(ServiceType type, ServiceBlockEntity be) {
    if (!be.isUnlimited() && type.sells() && stock(type, be) < be.getBatch()) {
      return Component.translatable("msg.roll_mod.service.out_of_stock");
    }
    return Component.translatable("msg.roll_mod.service.partial_batch", be.batchText());
  }

  public static Component done(ServiceType type, long amount, long cost, String owner) {
    return Component.translatable("msg.roll_mod.service.done." + type.id(), amount(type, amount), cost, owner);
  }

  public static Component ownerNotice(ServiceType type, long amount, long cost, String customer) {
    return Component.translatable("msg.roll_mod.service.owner." + type.id(), customer, amount(type, amount), cost);
  }

  public static Component insufficient(long cost) {
    return Component.translatable("msg.roll_mod.insufficient.currency", cost);
  }

  public static Component ownerCannotAfford() {
    return Component.translatable("msg.roll_mod.owner.broke");
  }

  public static Component changed() {
    return Component.translatable("msg.roll_mod.service.changed");
  }

  public static Component failed() {
    return Component.translatable("msg.roll_mod.transaction.failed");
  }

  /** {@code 1.5M EU}, {@code 120 XP}, {@code 4K mB}. */
  public static Component amount(ServiceType type, long amount) {
    return type.amountText(amount);
  }

  /** What the block holds to sell: EU, XP points or mB. */
  public static long stock(ServiceType type, ServiceBlockEntity be) {
    return switch (type.resource()) {
      case ENERGY -> be.getEnergy();
      case XP -> be.xpPoints();
      case FLUID -> be.getTank().getFluidAmount();
    };
  }

  /** Room the block has left to buy into, in the same measure as {@link #stock}. */
  public static long room(ServiceType type, ServiceBlockEntity be) {
    return switch (type.resource()) {
      case ENERGY -> be.getEnergyCapacity() - be.getEnergy();
      case XP -> (be.getTank().getCapacity() - be.getTank().getFluidAmount()) / ServiceType.MB_PER_XP_POINT;
      case FLUID -> be.getTank().getCapacity() - be.getTank().getFluidAmount();
    };
  }
}
