package com.roll_54.roll_mod.economy.vendingblock.auction;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * What a player did, or had done to them, on the Auction House: one row of their auction history
 * ({@code auction_history}, migration V5) and one line of {@code auction_actions.txt}.
 *
 * <p>Stored by {@link #name()}, both in the database and on the wire, so a value must never be
 * renamed or removed — old rows would no longer read back. Add new ones at the end.
 */
public enum AuctionAction {
  /** The player put an item up. Price: the fixed price, or the starting bid. */
  LISTED(ChatFormatting.AQUA),
  /** The player took their own listing down; the item went to their collection. */
  CANCELLED(ChatFormatting.GRAY),
  /** The player bought a fixed-price lot or bought an auction out. Price: what they paid. */
  BOUGHT(ChatFormatting.RED),
  /** Someone bought, bought out or won the player's lot. Price: what the buyer paid, before fee. */
  SOLD(ChatFormatting.GREEN),
  /** The player bid on an auction. Price: the bid, now held in escrow. */
  BID(ChatFormatting.GOLD),
  /** The player was outbid or bought out, and got their escrowed bid back. Price: that bid. */
  OUTBID_REFUND(ChatFormatting.YELLOW),
  /** The player's bid was highest when the auction ended. Price: the winning bid. */
  WON(ChatFormatting.GREEN),
  /** The player's lot ended unsold and went back to their collection. */
  EXPIRED_RETURNED(ChatFormatting.DARK_GRAY),
  /** The player took a stack out of their collection. No listing. */
  CLAIMED(ChatFormatting.WHITE);

  private final ChatFormatting color;

  AuctionAction(ChatFormatting color) {
    this.color = color;
  }

  /** The colour the history panel shows this action in. */
  public ChatFormatting color() {
    return color;
  }

  /** {@code gui.roll_mod.auction.history.action.<name in lower case>}, coloured. */
  public Component title() {
    return Component.translatable(
            "gui.roll_mod.auction.history.action." + name().toLowerCase(Locale.ROOT))
        .withStyle(color);
  }

  /** The action stored as {@code name}; {@link #LISTED} for one this build does not know. */
  public static AuctionAction byName(String name) {
    try {
      return valueOf(name);
    } catch (IllegalArgumentException | NullPointerException e) {
      return LISTED;
    }
  }
}
