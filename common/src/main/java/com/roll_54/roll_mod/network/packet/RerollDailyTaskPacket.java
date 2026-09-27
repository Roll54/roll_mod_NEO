package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: swap the daily task at {@code index} for another, paying its reroll price. */
public record RerollDailyTaskPacket(int index) implements CustomPacketPayload {

    public static final Type<RerollDailyTaskPacket> TYPE =
            new Type<>(RollMod.id("reroll_daily_task"));

    public static final StreamCodec<ByteBuf, RerollDailyTaskPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(RerollDailyTaskPacket::new, RerollDailyTaskPacket::index);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RerollDailyTaskPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // rerollSlot() re-validates and prices everything; a spoofed index is simply rejected.
                DailyTaskManager.rerollSlot(player, payload.index());
            }
        });
    }
}
