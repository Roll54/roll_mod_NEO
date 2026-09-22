package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.kits.ClientKitCache;
import com.roll_54.roll_mod.minestar.kits.KitView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** The kits one player may claim, with what is left of each cooldown. */
public record SyncKitsPacket(List<KitView> kits) implements CustomPacketPayload {

    /** A ceiling on a hand-made folder, so a mistake there cannot become an oversized packet. */
    private static final int MAX = 64;

    public static final Type<SyncKitsPacket> TYPE = new Type<>(RollMod.id("sync_kits"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncKitsPacket> STREAM_CODEC =
            StreamCodec.of(SyncKitsPacket::encode, SyncKitsPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncKitsPacket payload) {
        int count = Math.min(MAX, payload.kits().size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            payload.kits().get(i).encode(buf);
        }
    }

    private static SyncKitsPacket decode(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX, buf.readVarInt());
        List<KitView> kits = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            kits.add(KitView.decode(buf));
        }
        return new SyncKitsPacket(List.copyOf(kits));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncKitsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientKitCache.receivedAt = System.currentTimeMillis();
            ClientKitCache.KITS = payload.kits();
        });
    }
}
