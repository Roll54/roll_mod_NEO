package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.op.ClientOperatorCache;
import com.roll_54.roll_mod.minestar.op.OperatorEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** The operator roster, or an empty list for everyone who may not manage it. */
public record SyncOperatorsPacket(List<OperatorEntry> entries) implements CustomPacketPayload {

    private static final int MAX = 300;

    public static final Type<SyncOperatorsPacket> TYPE = new Type<>(RollMod.id("sync_operators"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOperatorsPacket> STREAM_CODEC =
            StreamCodec.of(SyncOperatorsPacket::encode, SyncOperatorsPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncOperatorsPacket payload) {
        int count = Math.min(MAX, payload.entries().size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            payload.entries().get(i).encode(buf);
        }
    }

    private static SyncOperatorsPacket decode(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX, buf.readVarInt());
        List<OperatorEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(OperatorEntry.decode(buf));
        }
        return new SyncOperatorsPacket(List.copyOf(entries));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncOperatorsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientOperatorCache.ENTRIES = payload.entries());
    }
}
