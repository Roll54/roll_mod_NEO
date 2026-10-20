package com.roll_54.roll_mod.netherstorm;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: whether the Nether storm is active. Purely cosmetic on the receiving end — it
 * only drives how nether portals are drawn.
 */
public record SyncStormPacket(boolean active) implements CustomPacketPayload {

    public static final Type<SyncStormPacket> TYPE = new Type<>(RollMod.id("sync_storm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStormPacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.<RegistryFriendlyByteBuf>cast().map(SyncStormPacket::new, SyncStormPacket::active);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncStormPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientStormCache.set(payload.active()));
    }
}
