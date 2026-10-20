package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.friends.ClientFriendsCache;
import com.roll_54.roll_mod.minestar.friends.FriendDetail;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Server → client: the profile of the friend selected in the friends tab, or word that it could not
 * be loaded ({@code detail == null}).
 */
public record SyncFriendDetailPacket(UUID id, @Nullable FriendDetail detail) implements CustomPacketPayload {

    public static final Type<SyncFriendDetailPacket> TYPE = new Type<>(RollMod.id("sync_friend_detail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFriendDetailPacket> STREAM_CODEC =
            StreamCodec.of(SyncFriendDetailPacket::encode, SyncFriendDetailPacket::decode);

    public static SyncFriendDetailPacket of(FriendDetail detail) {
        return new SyncFriendDetailPacket(detail.id(), detail);
    }

    public static SyncFriendDetailPacket failed(UUID id) {
        return new SyncFriendDetailPacket(id, null);
    }

    private static void encode(RegistryFriendlyByteBuf buf, SyncFriendDetailPacket payload) {
        buf.writeUUID(payload.id());
        buf.writeBoolean(payload.detail() != null);
        if (payload.detail() != null) payload.detail().encode(buf);
    }

    private static SyncFriendDetailPacket decode(RegistryFriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        return new SyncFriendDetailPacket(id, buf.readBoolean() ? FriendDetail.decode(buf) : null);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncFriendDetailPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.detail() != null) {
                ClientFriendsCache.DETAILS.put(payload.id(), payload.detail());
                ClientFriendsCache.FAILED.remove(payload.id());
            } else {
                ClientFriendsCache.FAILED.add(payload.id());
            }
            ClientFriendsCache.VERSION++;
        });
    }
}
