package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.friends.ClientFriendsCache;
import com.roll_54.roll_mod.minestar.friends.FriendEntry;
import com.roll_54.roll_mod.minestar.friends.FriendRequest;
import com.roll_54.roll_mod.minestar.friends.FriendsService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Server → client: the viewer's Minestar friends, with this server's flags. See {@link FriendsService}. */
public record SyncFriendsPacket(FriendsService.Status status, List<FriendEntry> entries,
                                List<FriendRequest> incoming, List<FriendRequest> outgoing)
        implements CustomPacketPayload {

    private static final int MAX_FRIENDS = 500;
    private static final int MAX_REQUESTS = 100;

    public static final Type<SyncFriendsPacket> TYPE = new Type<>(RollMod.id("sync_friends"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFriendsPacket> STREAM_CODEC =
            StreamCodec.of(SyncFriendsPacket::encode, SyncFriendsPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncFriendsPacket payload) {
        buf.writeVarInt(payload.status().ordinal());
        int count = Math.min(MAX_FRIENDS, payload.entries().size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) payload.entries().get(i).encode(buf);
        writeRequests(buf, payload.incoming());
        writeRequests(buf, payload.outgoing());
    }

    private static void writeRequests(RegistryFriendlyByteBuf buf, List<FriendRequest> requests) {
        int count = Math.min(MAX_REQUESTS, requests.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) requests.get(i).encode(buf);
    }

    private static List<FriendRequest> readRequests(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX_REQUESTS, buf.readVarInt());
        List<FriendRequest> requests = new ArrayList<>(count);
        for (int i = 0; i < count; i++) requests.add(FriendRequest.decode(buf));
        return List.copyOf(requests);
    }

    private static SyncFriendsPacket decode(RegistryFriendlyByteBuf buf) {
        // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
        FriendsService.Status[] statuses = FriendsService.Status.values();
        FriendsService.Status status = statuses[Math.floorMod(buf.readVarInt(), statuses.length)];
        int count = Math.min(MAX_FRIENDS, buf.readVarInt());
        List<FriendEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(FriendEntry.decode(buf));
        return new SyncFriendsPacket(status, List.copyOf(entries), readRequests(buf), readRequests(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncFriendsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientFriendsCache.STATUS = payload.status();
            ClientFriendsCache.ENTRIES = payload.entries();
            ClientFriendsCache.INCOMING = payload.incoming();
            ClientFriendsCache.OUTGOING = payload.outgoing();
            ClientFriendsCache.VERSION++;
        });
    }
}
