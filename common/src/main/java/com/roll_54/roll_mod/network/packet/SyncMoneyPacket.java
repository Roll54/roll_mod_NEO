package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.model.CurrencyOffer;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.model.MoneyRequest;
import com.roll_54.roll_mod.economy.network.client.ClientMoneyCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One player's money offers and requests, both ways — what the player interactions tab answers. */
public record SyncMoneyPacket(List<CurrencyOffer> offersIn, List<CurrencyOffer> offersOut,
                              List<MoneyRequest> requestsIn, List<MoneyRequest> requestsOut)
        implements CustomPacketPayload {

    private static final int MAX = 64;

    public static final Type<SyncMoneyPacket> TYPE = new Type<>(RollMod.id("sync_money"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMoneyPacket> STREAM_CODEC =
            StreamCodec.of(SyncMoneyPacket::encode, SyncMoneyPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncMoneyPacket payload) {
        writeOffers(buf, payload.offersIn());
        writeOffers(buf, payload.offersOut());
        writeRequests(buf, payload.requestsIn());
        writeRequests(buf, payload.requestsOut());
    }

    private static SyncMoneyPacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncMoneyPacket(readOffers(buf), readOffers(buf), readRequests(buf), readRequests(buf));
    }

    private static void writeOffers(RegistryFriendlyByteBuf buf, List<CurrencyOffer> offers) {
        int count = Math.min(MAX, offers.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            CurrencyOffer offer = offers.get(i);
            buf.writeUUID(offer.id());
            buf.writeUUID(offer.sender());
            buf.writeUtf(offer.senderName(), 16);
            buf.writeUUID(offer.recipient());
            buf.writeUtf(offer.recipientName(), 16);
            buf.writeUtf(offer.type().id(), 32);
            buf.writeVarLong(offer.amount());
            buf.writeLong(offer.expiresAt());
        }
    }

    private static List<CurrencyOffer> readOffers(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX, buf.readVarInt());
        List<CurrencyOffer> offers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            var id = buf.readUUID();
            var sender = buf.readUUID();
            String senderName = buf.readUtf(16);
            var recipient = buf.readUUID();
            String recipientName = buf.readUtf(16);
            Optional<CurrencyType> type = CurrencyType.byId(buf.readUtf(32));
            long amount = buf.readVarLong();
            long expiresAt = buf.readLong();
            type.ifPresent(t -> offers.add(new CurrencyOffer(id, sender, senderName, recipient, recipientName,
                    t, amount, expiresAt)));
        }
        return List.copyOf(offers);
    }

    private static void writeRequests(RegistryFriendlyByteBuf buf, List<MoneyRequest> requests) {
        int count = Math.min(MAX, requests.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            requests.get(i).encode(buf);
        }
    }

    private static List<MoneyRequest> readRequests(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX, buf.readVarInt());
        List<MoneyRequest> requests = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            MoneyRequest.decode(buf).ifPresent(requests::add);
        }
        return List.copyOf(requests);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncMoneyPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientMoneyCache.OFFERS_IN = payload.offersIn();
            ClientMoneyCache.OFFERS_OUT = payload.offersOut();
            ClientMoneyCache.REQUESTS_IN = payload.requestsIn();
            ClientMoneyCache.REQUESTS_OUT = payload.requestsOut();
            ClientMoneyCache.VERSION++;
        });
    }
}
