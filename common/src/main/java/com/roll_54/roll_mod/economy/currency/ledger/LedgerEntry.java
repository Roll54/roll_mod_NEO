package com.roll_54.roll_mod.economy.currency.ledger;

import net.minecraft.network.FriendlyByteBuf;

/**
 * One row of a player's money history, as the client shows it.
 *
 * @param item      the item's display name for a trade, empty otherwise
 * @param otherName the other side of a trade, empty when there was none
 * @param at        epoch millis
 */
public record LedgerEntry(long delta, LedgerReason reason, String item, int count, String otherName, long at) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarLong(delta >= 0 ? delta : 0);
        buf.writeVarLong(delta < 0 ? -delta : 0);
        buf.writeUtf(reason.name(), 32);
        buf.writeUtf(item, 256);
        buf.writeVarInt(count);
        buf.writeUtf(otherName, 16);
        buf.writeLong(at);
    }

    public static LedgerEntry decode(FriendlyByteBuf buf) {
        long plus = buf.readVarLong();
        long minus = buf.readVarLong();
        return new LedgerEntry(plus - minus, LedgerReason.byName(buf.readUtf(32)), buf.readUtf(256),
                buf.readVarInt(), buf.readUtf(16), buf.readLong());
    }
}
