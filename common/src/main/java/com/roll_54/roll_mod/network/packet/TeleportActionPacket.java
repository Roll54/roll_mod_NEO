package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.rtp.RtpService;
import com.roll_54.roll_mod.minestar.teleport.TeleportService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The hub's three argument-less trips: random teleport, back, spawn.
 *
 * <p>One payload for all three because each is a button with nothing to say but which button it
 * was, and each lands in the same service the matching command calls — so a click and a typed
 * command are refused for exactly the same reasons.
 */
public record TeleportActionPacket(Action action) implements CustomPacketPayload {

    public enum Action { RTP, BACK, SPAWN }

    public static final Type<TeleportActionPacket> TYPE = new Type<>(RollMod.id("teleport_action"));

    public static final StreamCodec<ByteBuf, TeleportActionPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(
                    // Modulo rather than a bounds check: a crafted ordinal must not throw here.
                    ordinal -> new TeleportActionPacket(
                            Action.values()[Math.floorMod(ordinal, Action.values().length)]),
                    payload -> payload.action().ordinal());

    public static TeleportActionPacket of(Action action) {
        return new TeleportActionPacket(action);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TeleportActionPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action()) {
                case RTP -> RtpService.request(player);
                case BACK -> TeleportService.back(player);
                case SPAWN -> TeleportService.spawn(player);
            }
        });
    }
}
