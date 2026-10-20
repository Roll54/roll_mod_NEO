package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyOfferService;
import com.roll_54.roll_mod.economy.api.MoneyRequestService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.util.PlayerLookup;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * What the player interactions tab's money buttons do.
 *
 * <p>{@code subject} is the other player for {@link Action#SEND}/{@link Action#REQUEST}, and the
 * offer or request id for every answer. The services check that the sender is a party to whatever
 * they name, so a crafted id can only reach the sender's own rows.
 */
public record MoneyActionPacket(Action action, UUID subject, long amount) implements CustomPacketPayload {

    public enum Action { SEND, REQUEST, ACCEPT_OFFER, DENY_OFFER, PAY_REQUEST, REFUSE_REQUEST, CANCEL_REQUEST }

    public static final Type<MoneyActionPacket> TYPE = new Type<>(RollMod.id("money_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoneyActionPacket> STREAM_CODEC =
            StreamCodec.of(MoneyActionPacket::encode, MoneyActionPacket::decode);

    public static MoneyActionPacket send(UUID to, long amount, boolean request) {
        return new MoneyActionPacket(request ? Action.REQUEST : Action.SEND, to, amount);
    }

    public static MoneyActionPacket answer(Action action, UUID id) {
        return new MoneyActionPacket(action, id, 0L);
    }

    private static void encode(RegistryFriendlyByteBuf buf, MoneyActionPacket payload) {
        buf.writeVarInt(payload.action().ordinal());
        buf.writeUUID(payload.subject());
        buf.writeVarLong(payload.amount());
    }

    private static MoneyActionPacket decode(RegistryFriendlyByteBuf buf) {
        // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
        Action action = Action.values()[Math.floorMod(buf.readVarInt(), Action.values().length)];
        return new MoneyActionPacket(action, buf.readUUID(), buf.readVarLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MoneyActionPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action()) {
                case SEND, REQUEST -> {
                    if (payload.amount() <= 0) return;
                    PlayerLookup.Profile other = profile(player.server, payload.subject());
                    if (other == null) return;
                    if (payload.action() == Action.SEND) {
                        CurrencyOfferService.offer(player, other, CurrencyType.MAIN, payload.amount());
                    } else {
                        MoneyRequestService.request(player, other, CurrencyType.MAIN, payload.amount());
                    }
                }
                case ACCEPT_OFFER -> CurrencyOfferService.answer(player, payload.subject(), true);
                case DENY_OFFER -> CurrencyOfferService.answer(player, payload.subject(), false);
                case PAY_REQUEST -> MoneyRequestService.pay(player, payload.subject());
                case REFUSE_REQUEST -> MoneyRequestService.refuse(player, payload.subject());
                case CANCEL_REQUEST -> MoneyRequestService.cancel(player, payload.subject());
            }
        });
    }

    /**
     * The other party by uuid: online, or anyone who has joined this server before. The name comes
     * from the server, never from the packet, so what is stored on the row is the real spelling.
     */
    @Nullable
    private static PlayerLookup.Profile profile(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) return new PlayerLookup.Profile(id, online.getGameProfile().getName());
        var cache = server.getProfileCache();
        if (cache == null) return null;
        return cache.get(id).map(p -> new PlayerLookup.Profile(p.getId(), p.getName())).orElse(null);
    }
}
