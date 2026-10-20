package com.roll_54.roll_mod.economy.currency.model;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Money one player has asked another to pay them, waiting for the payer's answer.
 *
 * <p>The opposite of a {@link CurrencyOffer}: nothing is held. The payer's coins stay theirs until
 * they agree, and are taken only then — so a request can lapse, be refused or be withdrawn by
 * simply deleting it.
 *
 * <p>Both names are stored, as on offers, so a request can be listed and answered by name while the
 * other party is offline.
 */
public record MoneyRequest(UUID id, UUID requester, String requesterName, UUID payer, String payerName,
                           CurrencyType type, long amount, long expiresAt) {

    public boolean expired(long now) {
        return now >= expiresAt;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUUID(requester);
        buf.writeUtf(requesterName, 16);
        buf.writeUUID(payer);
        buf.writeUtf(payerName, 16);
        buf.writeUtf(type.id(), 32);
        buf.writeVarLong(amount);
        buf.writeLong(expiresAt);
    }

    /** Empty for a currency this build does not know — the row is skipped, not the packet. */
    public static Optional<MoneyRequest> decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        UUID requester = buf.readUUID();
        String requesterName = buf.readUtf(16);
        UUID payer = buf.readUUID();
        String payerName = buf.readUtf(16);
        String type = buf.readUtf(32);
        long amount = buf.readVarLong();
        long expiresAt = buf.readLong();
        return CurrencyType.byId(type).map(t ->
                new MoneyRequest(id, requester, requesterName, payer, payerName, t, amount, expiresAt));
    }
}
