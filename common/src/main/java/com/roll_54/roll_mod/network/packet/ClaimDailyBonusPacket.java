package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: take the all-complete bonus reward.
 *
 * <p>Carries nothing. Which reward is owed, and whether the sender is owed it at all, is entirely
 * the server's business — see {@link DailyTaskManager#claimBonus}.
 */
public record ClaimDailyBonusPacket() implements CustomPacketPayload {

    public static final Type<ClaimDailyBonusPacket> TYPE =
            new Type<>(RollMod.id("claim_daily_bonus"));

    public static final StreamCodec<ByteBuf, ClaimDailyBonusPacket> STREAM_CODEC =
            StreamCodec.unit(new ClaimDailyBonusPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClaimDailyBonusPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // claimBonus() re-validates everything; an unearned claim is simply rejected.
                DailyTaskManager.claimBonus(player);
            }
        });
    }
}
