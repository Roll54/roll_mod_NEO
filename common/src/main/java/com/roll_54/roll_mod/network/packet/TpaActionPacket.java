package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.tpa.TpaRequest;
import com.roll_54.roll_mod.minestar.tpa.TpaService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * What the tpa tab's buttons do.
 *
 * <p>{@code target} is the player a new request is for; {@code request} is the one an answer is
 * about. Only one of the two matters per action, and the service checks that the sender is actually
 * a party to whatever they name.
 */
public record TpaActionPacket(Action action, UUID target, UUID request) implements CustomPacketPayload {

    public enum Action { REQUEST, REQUEST_HERE, ACCEPT, DENY, CANCEL }

    private static final UUID NONE = new UUID(0L, 0L);

    public static final Type<TpaActionPacket> TYPE = new Type<>(RollMod.id("tpa_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TpaActionPacket> STREAM_CODEC =
            StreamCodec.of(TpaActionPacket::encode, TpaActionPacket::decode);

    public static TpaActionPacket request(UUID target, boolean here) {
        return new TpaActionPacket(here ? Action.REQUEST_HERE : Action.REQUEST, target, NONE);
    }

    public static TpaActionPacket answer(UUID request, boolean accept) {
        return new TpaActionPacket(accept ? Action.ACCEPT : Action.DENY, NONE, request);
    }

    public static TpaActionPacket cancel(UUID request) {
        return new TpaActionPacket(Action.CANCEL, NONE, request);
    }

    private static void encode(RegistryFriendlyByteBuf buf, TpaActionPacket payload) {
        buf.writeVarInt(payload.action().ordinal());
        buf.writeUUID(payload.target());
        buf.writeUUID(payload.request());
    }

    private static TpaActionPacket decode(RegistryFriendlyByteBuf buf) {
        // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
        Action action = Action.values()[Math.floorMod(buf.readVarInt(), Action.values().length)];
        return new TpaActionPacket(action, buf.readUUID(), buf.readUUID());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TpaActionPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action()) {
                case REQUEST, REQUEST_HERE -> {
                    ServerPlayer target = player.server.getPlayerList().getPlayer(payload.target());
                    if (target != null) {
                        TpaService.request(player, target,
                                payload.action() == Action.REQUEST_HERE
                                        ? TpaRequest.Kind.HERE
                                        : TpaRequest.Kind.TO);
                    }
                }
                case ACCEPT -> TpaService.resolve(player, payload.request(), true);
                case DENY, CANCEL -> TpaService.resolve(player, payload.request(), false);
            }
        });
    }
}
