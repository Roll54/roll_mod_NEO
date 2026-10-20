package com.roll_54.roll_mod.economy.vendingblock.auction;

import net.minecraft.network.FriendlyByteBuf;

/**
 * One row of a player's auction history, as the client shows it.
 *
 * @param item      the item's registry id ({@code minecraft:diamond}), empty when unknown; the
 *                  client turns it into an icon and a name in its own language
 * @param price     the money involved, 0 when none (see {@link AuctionAction} for what it means)
 * @param otherName the other side, empty when there was none
 * @param at        epoch millis
 */
public record AuctionHistoryEntry(AuctionAction action, String item, int count, long price,
                                  String otherName, long at) {

  public void encode(FriendlyByteBuf buf) {
    buf.writeUtf(action.name(), 32);
    buf.writeUtf(item, 256);
    buf.writeVarInt(count);
    buf.writeVarLong(Math.max(0, price));
    buf.writeUtf(otherName, 16);
    buf.writeLong(at);
  }

  public static AuctionHistoryEntry decode(FriendlyByteBuf buf) {
    return new AuctionHistoryEntry(AuctionAction.byName(buf.readUtf(32)), buf.readUtf(256),
        buf.readVarInt(), buf.readVarLong(), buf.readUtf(16), buf.readLong());
  }
}
