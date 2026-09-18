package com.roll_54.roll_mod.economy.vendingblock.gui.chat;

import net.minecraft.network.chat.Component;

public class Messages {

  // Currency-based messages
  public static Component playerBoughtCurrency(
      int count, Component item, String owner, long price) {
    return Component.translatable("msg.roll_mod.sell.currency", count, item, owner, price);
  }

  public static Component ownerSoldCurrency(int count, Component item, String player, long price) {
    return Component.translatable(
        "msg.roll_mod.sell.owner.currency", count, item, player, price);
  }

  public static Component playerInsufficientCurrency(long required) {
    return Component.translatable("msg.roll_mod.insufficient.currency", required);
  }

  // Buy-mode (owner buys from players) currency messages
  public static Component playerSoldCurrency(int count, Component item, String owner, long price) {
    return Component.translatable(
        "msg.roll_mod.buy.player.currency", count, item, owner, price);
  }

  public static Component ownerBoughtCurrency(
      int count, Component item, String player, long price) {
    return Component.translatable(
        "msg.roll_mod.buy.owner.currency", count, item, player, price);
  }

  public static Component ownerCannotAfford() {
    return Component.translatable("msg.roll_mod.owner.broke");
  }

  public static Component transactionFailed() {
    return Component.translatable("msg.roll_mod.transaction.failed");
  }

  // Legacy item-based messages (kept for compatibility if needed)
  public static Component playerBought(
      int count, Component item, String owner, int sellCount, Component sellItem) {
    return Component.translatable(
        "msg.roll_mod.sell", count, item, owner, sellCount, sellItem);
  }

  public static Component playerRequest(int count, Component item, String owner) {
    return Component.translatable("msg.roll_mod.request", count, item, owner);
  }

  public static Component playerGiveaway(int count, Component item, String owner) {
    return Component.translatable("msg.roll_mod.giveaway", count, item, owner);
  }

  public static Component playerEmpty(Component sellItem) {
    return Component.translatable("msg.roll_mod.empty.player", sellItem);
  }

  public static Component playerFull() {
    return Component.translatable("msg.roll_mod.full.player");
  }

  public static Component vendorFull() {
    return Component.translatable("msg.roll_mod.full");
  }

  public static Component vendorSold() {
    return Component.translatable("msg.roll_mod.sold");
  }

  public static Component vendorEmpty() {
    return Component.translatable("msg.roll_mod.empty");
  }

  public static Component ownerSold(
      int count, Component item, String player, int sellCount, Component sellItem) {
    return Component.translatable(
        "msg.roll_mod.sell.owner", count, item, player, sellCount, sellItem);
  }

  public static Component ownerRequest(int count, Component item, String player) {
    return Component.translatable("msg.roll_mod.request.owner", count, item, player);
  }

  public static Component ownerGiveaway(int count, Component item, String player) {
    return Component.translatable("msg.roll_mod.giveaway.owner", count, item, player);
  }

  public static Component ownerSold() {
    return Component.translatable("msg.roll_mod.sold.owner");
  }

  public static Component ownerFull() {
    return Component.translatable("msg.roll_mod.full.owner");
  }

  public static Component blacklistedProduct(String item) {
    return Component.translatable("msg.roll_mod.blacklist.product", item);
  }

  public static Component blacklistedFacade(String item) {
    return Component.translatable("msg.roll_mod.blacklist.facade", item);
  }

  public static Component fullBlockFacade(String item) {
    return Component.translatable("msg.roll_mod.blacklist.fullBlock", item);
  }

  // ----------------------------------------------- Auction House
  // ------------------------------------------------
  public static Component ahListed(int count, Component item) {
    return Component.translatable("msg.roll_mod.ah.listed", count, item);
  }

  public static Component ahMaxListings(int max) {
    return Component.translatable("msg.roll_mod.ah.maxListings", max);
  }

  public static Component ahDurationCapped(int days) {
    return Component.translatable("msg.roll_mod.ah.durationCapped", days);
  }

  public static Component ahInvalidPrice() {
    return Component.translatable("msg.roll_mod.ah.invalidPrice");
  }

  public static Component ahNotFound() {
    return Component.translatable("msg.roll_mod.ah.notFound");
  }

  public static Component ahOwnListing() {
    return Component.translatable("msg.roll_mod.ah.ownListing");
  }

  public static Component ahNotOwner() {
    return Component.translatable("msg.roll_mod.ah.notOwner");
  }

  public static Component ahBought(int count, Component item, long price) {
    return Component.translatable("msg.roll_mod.ah.bought", count, item, price);
  }

  public static Component ahSold(int count, Component item, String buyer, long price) {
    return Component.translatable("msg.roll_mod.ah.sold", count, item, buyer, price);
  }

  public static Component ahBidPlaced(Component item, long amount) {
    return Component.translatable("msg.roll_mod.ah.bidPlaced", amount, item);
  }

  public static Component ahBidTooLow(long min) {
    return Component.translatable("msg.roll_mod.ah.bidTooLow", min);
  }

  public static Component ahOutbid(Component item) {
    return Component.translatable("msg.roll_mod.ah.outbid", item);
  }

  public static Component ahWon(int count, Component item, long price) {
    return Component.translatable("msg.roll_mod.ah.won", count, item, price);
  }

  public static Component ahExpiredReturned(int count, Component item) {
    return Component.translatable("msg.roll_mod.ah.expired", count, item);
  }

  public static Component ahCancelled(int count, Component item) {
    return Component.translatable("msg.roll_mod.ah.cancelled", count, item);
  }

  public static Component ahCannotCancel() {
    return Component.translatable("msg.roll_mod.ah.cannotCancel");
  }

  public static Component ahNothingToClaim() {
    return Component.translatable("msg.roll_mod.ah.nothingToClaim");
  }

  public static Component ahClaimed() {
    return Component.translatable("msg.roll_mod.ah.claimed");
  }

  public static Component ahClaimedPartial() {
    return Component.translatable("msg.roll_mod.ah.claimedPartial");
  }

  public static Component ahToCollection() {
    return Component.translatable("msg.roll_mod.ah.toCollection");
  }
}
