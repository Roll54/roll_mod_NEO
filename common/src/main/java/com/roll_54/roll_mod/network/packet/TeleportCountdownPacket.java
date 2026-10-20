package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.teleport.ClientTeleportCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: a warp warm-up started ({@code seconds > 0}) or stopped ({@code 0}). Drives the
 * hub's countdown window; the action-bar countdown is independent of it and keeps running.
 */
public record TeleportCountdownPacket(int seconds, String destination) implements CustomPacketPayload {

    public static final Type<TeleportCountdownPacket> TYPE = new Type<>(RollMod.id("teleport_countdown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportCountdownPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TeleportCountdownPacket::seconds,
                    ByteBufCodecs.STRING_UTF8, TeleportCountdownPacket::destination,
                    TeleportCountdownPacket::new);

    public static TeleportCountdownPacket stopped() {
        return new TeleportCountdownPacket(0, "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TeleportCountdownPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientTeleportCache.ENDS_AT = payload.seconds <= 0 ? 0L
                    : System.currentTimeMillis() + payload.seconds * 1000L;
            ClientTeleportCache.DESTINATION = payload.destination;
        });
    }
}
