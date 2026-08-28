package com.roll_54.roll_mod.network.packet;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.gui.DailyTasksUI;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: the player pressed the daily-tasks key. The screen is a LdLib2 container UI, so
 * it has to be opened server-side.
 */
public record OpenDailyTasksPacket() implements CustomPacketPayload {

    public static final Type<OpenDailyTasksPacket> TYPE =
            new Type<>(RollMod.id("open_daily_tasks"));

    public static final StreamCodec<ByteBuf, OpenDailyTasksPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenDailyTasksPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenDailyTasksPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerUIMenuType.openUI(player, DailyTasksUI.UI_ID);
            }
        });
    }
}
