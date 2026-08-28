package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: take the reward for the daily task at {@code index}. */
public record ClaimDailyTaskPacket(int index) implements CustomPacketPayload {

    public static final Type<ClaimDailyTaskPacket> TYPE =
            new Type<>(RollMod.id("claim_daily_task"));

    public static final StreamCodec<ByteBuf, ClaimDailyTaskPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ClaimDailyTaskPacket::new, ClaimDailyTaskPacket::index);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClaimDailyTaskPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // claim() re-validates everything; a spoofed index is simply rejected.
                DailyTaskManager.claim(player, payload.index());
            }
        });
    }
}
