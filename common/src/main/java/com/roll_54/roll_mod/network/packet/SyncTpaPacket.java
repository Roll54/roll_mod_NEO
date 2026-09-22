package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.tpa.ClientTpaCache;
import com.roll_54.roll_mod.minestar.tpa.TpaMode;
import com.roll_54.roll_mod.minestar.tpa.TpaRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One player's teleport requests, who else is online to send one to, and what they let through.
 *
 * <p>The mode rides along here rather than in a payload of its own: this one is already built per
 * recipient and already re-sent on every change, and two packets for one screen could only arrive
 * in the wrong order.
 */
public record SyncTpaPacket(List<TpaRequest> incoming, List<TpaRequest> outgoing,
                            Map<UUID, String> online, TpaMode mode) implements CustomPacketPayload {

    /** Caps: a busy server's roster still has to fit comfortably in one payload. */
    private static final int MAX_REQUESTS = 64;
    private static final int MAX_ONLINE = 300;

    public static final Type<SyncTpaPacket> TYPE = new Type<>(RollMod.id("sync_tpa"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTpaPacket> STREAM_CODEC =
            StreamCodec.of(SyncTpaPacket::encode, SyncTpaPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncTpaPacket payload) {
        writeRequests(buf, payload.incoming());
        writeRequests(buf, payload.outgoing());

        int count = Math.min(MAX_ONLINE, payload.online().size());
        buf.writeVarInt(count);
        int written = 0;
        for (Map.Entry<UUID, String> entry : payload.online().entrySet()) {
            if (written++ >= count) break;
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue(), 16);
        }

        buf.writeVarInt(payload.mode().ordinal());
    }

    private static SyncTpaPacket decode(RegistryFriendlyByteBuf buf) {
        List<TpaRequest> incoming = readRequests(buf);
        List<TpaRequest> outgoing = readRequests(buf);

        int count = Math.min(MAX_ONLINE, buf.readVarInt());
        Map<UUID, String> online = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            online.put(buf.readUUID(), buf.readUtf(16));
        }

        // Modulo rather than a bounds check, as elsewhere: a crafted ordinal must not throw on the
        // network thread.
        TpaMode mode = TpaMode.values()[Math.floorMod(buf.readVarInt(), TpaMode.values().length)];
        return new SyncTpaPacket(incoming, outgoing, Map.copyOf(online), mode);
    }

    private static void writeRequests(RegistryFriendlyByteBuf buf, List<TpaRequest> requests) {
        int count = Math.min(MAX_REQUESTS, requests.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            requests.get(i).encode(buf);
        }
    }

    private static List<TpaRequest> readRequests(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX_REQUESTS, buf.readVarInt());
        List<TpaRequest> requests = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            requests.add(TpaRequest.decode(buf));
        }
        return List.copyOf(requests);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTpaPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientTpaCache.INCOMING = payload.incoming();
            ClientTpaCache.OUTGOING = payload.outgoing();
            ClientTpaCache.ONLINE = payload.online();
            ClientTpaCache.MODE = payload.mode();
        });
    }
}
