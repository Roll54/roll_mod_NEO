package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.moderation.ClientPlayerStatusCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The receiving player's own standing: what the hub button badges and the home tab reports.
 *
 * <p>{@code muteRemaining} is a duration, not a deadline — {@code -1} not muted, {@code 0} muted
 * with no end — so the client's clock never has to agree with the server's. The cache turns it into
 * a local deadline on receipt.
 */
public record SyncPlayerStatusPacket(int warns, long muteRemaining, int unreadLetters,
                                     boolean moderator, int dailyClaimable) implements CustomPacketPayload {

    public static final Type<SyncPlayerStatusPacket> TYPE = new Type<>(RollMod.id("sync_player_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerStatusPacket> STREAM_CODEC =
            StreamCodec.of(SyncPlayerStatusPacket::encode, SyncPlayerStatusPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncPlayerStatusPacket payload) {
        buf.writeVarInt(payload.warns());
        buf.writeVarLong(payload.muteRemaining() + 1L);   // +1 so the -1 sentinel stays a varlong
        buf.writeVarInt(payload.unreadLetters());
        buf.writeBoolean(payload.moderator());
        buf.writeVarInt(payload.dailyClaimable());
    }

    private static SyncPlayerStatusPacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncPlayerStatusPacket(buf.readVarInt(), buf.readVarLong() - 1L,
                buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPlayerStatusPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayerStatusCache.accept(payload));
    }
}
