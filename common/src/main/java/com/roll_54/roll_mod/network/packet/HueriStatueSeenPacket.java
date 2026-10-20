package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.ClientHueriStatue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: whether this player has opened a Hueri statue yet, which is what the statue's
 * "not read yet" particles, "!" and hint key off. Sent on login and on the first open.
 */
public record HueriStatueSeenPacket(boolean seen) implements CustomPacketPayload {

    public static final Type<HueriStatueSeenPacket> TYPE = new Type<>(RollMod.id("hueri_statue_seen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HueriStatueSeenPacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(HueriStatueSeenPacket::new, HueriStatueSeenPacket::seen).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HueriStatueSeenPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHueriStatue.seen = payload.seen);
    }
}
