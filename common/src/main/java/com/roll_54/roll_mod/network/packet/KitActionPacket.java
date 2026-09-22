package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.kits.KitService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * "Give me that kit."
 *
 * <p>Only the name travels: the service re-checks the permission and the cooldown, so a crafted
 * packet buys nothing that {@code /kit} would not have given the same player anyway.
 */
public record KitActionPacket(String kit) implements CustomPacketPayload {

    public static final Type<KitActionPacket> TYPE = new Type<>(RollMod.id("kit_action"));

    public static final StreamCodec<ByteBuf, KitActionPacket> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(32).map(KitActionPacket::new, KitActionPacket::kit);

    public static KitActionPacket claim(String kit) {
        return new KitActionPacket(kit);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KitActionPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                KitService.claim(player, payload.kit());
            }
        });
    }
}
