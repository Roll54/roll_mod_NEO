package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.welcome.WelcomeService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: the player's answer on the starting screen. Carries no authority —
 * {@link WelcomeService} only acts on it while it has the screen open for that player.
 */
public record WelcomeResponsePacket(boolean accepted) implements CustomPacketPayload {

    public static final Type<WelcomeResponsePacket> TYPE = new Type<>(RollMod.id("welcome_response"));

    public static final StreamCodec<ByteBuf, WelcomeResponsePacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(WelcomeResponsePacket::new, WelcomeResponsePacket::accepted);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WelcomeResponsePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.accepted()) {
                    WelcomeService.accept(player);
                } else {
                    WelcomeService.decline(player);
                }
            }
        });
    }
}
