package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.tpa.TpaMode;
import com.roll_54.roll_mod.minestar.tpa.TpaSettings;
import com.roll_54.roll_mod.minestar.tpa.TpaViewers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * "Only these people may ask me for a teleport."
 *
 * <p>A payload of its own rather than another {@code TpaActionPacket} action: that one carries two
 * UUIDs and this carries neither, and every accept and deny would grow three dead bytes to make
 * room for a field only this uses.
 *
 * <p>Nothing to authorise — a player owns their own setting — so the only check is that the value
 * is a real one, which the decode guarantees.
 */
public record TpaModePacket(TpaMode mode) implements CustomPacketPayload {

    public static final Type<TpaModePacket> TYPE = new Type<>(RollMod.id("tpa_mode"));

    public static final StreamCodec<ByteBuf, TpaModePacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(
                    // Modulo rather than a bounds check: a crafted ordinal must not throw here.
                    ordinal -> new TpaModePacket(
                            TpaMode.values()[Math.floorMod(ordinal, TpaMode.values().length)]),
                    payload -> payload.mode().ordinal());

    public static TpaModePacket of(TpaMode mode) {
        return new TpaModePacket(mode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TpaModePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            TpaSettings.set(player, payload.mode());
            player.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.modeSet",
                    payload.mode().title()));
            // The window shows what comes back, not what was clicked.
            TpaViewers.syncTo(player);
        });
    }
}
