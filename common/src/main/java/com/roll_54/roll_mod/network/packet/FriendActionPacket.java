package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.friends.FriendsService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Client → server: what the friends tab asks for. {@code friend} is ignored by {@link Action#REFRESH}.
 * Carries no authority — {@link FriendsService} only acts on friends in the sender's own list, or, for
 * selecting, inviting and adding, on players online on this server.
 */
public record FriendActionPacket(Action action, UUID friend) implements CustomPacketPayload {

    public enum Action { SELECT, INVITE, REFRESH, ADD, REMOVE, ACCEPT, DECLINE, CANCEL }

    private static final UUID NONE = new UUID(0L, 0L);

    public static final Type<FriendActionPacket> TYPE = new Type<>(RollMod.id("friend_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FriendActionPacket> STREAM_CODEC =
            StreamCodec.of(FriendActionPacket::encode, FriendActionPacket::decode);

    public static FriendActionPacket select(UUID friend) {
        return new FriendActionPacket(Action.SELECT, friend);
    }

    public static FriendActionPacket invite(UUID friend) {
        return new FriendActionPacket(Action.INVITE, friend);
    }

    public static FriendActionPacket add(UUID target) {
        return new FriendActionPacket(Action.ADD, target);
    }

    public static FriendActionPacket remove(UUID friend) {
        return new FriendActionPacket(Action.REMOVE, friend);
    }

    /** Answers an incoming request. */
    public static FriendActionPacket answer(UUID from, boolean accept) {
        return new FriendActionPacket(accept ? Action.ACCEPT : Action.DECLINE, from);
    }

    /** Withdraws an outgoing request. */
    public static FriendActionPacket cancel(UUID to) {
        return new FriendActionPacket(Action.CANCEL, to);
    }

    public static FriendActionPacket refresh() {
        return new FriendActionPacket(Action.REFRESH, NONE);
    }

    private static void encode(RegistryFriendlyByteBuf buf, FriendActionPacket payload) {
        buf.writeVarInt(payload.action().ordinal());
        buf.writeUUID(payload.friend());
    }

    private static FriendActionPacket decode(RegistryFriendlyByteBuf buf) {
        // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
        Action action = Action.values()[Math.floorMod(buf.readVarInt(), Action.values().length)];
        return new FriendActionPacket(action, buf.readUUID());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FriendActionPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action()) {
                case SELECT -> FriendsService.select(player, payload.friend());
                case INVITE -> FriendsService.invite(player, payload.friend());
                case REFRESH -> FriendsService.refresh(player, true);
                case ADD -> FriendsService.addFriend(player, payload.friend());
                case REMOVE -> FriendsService.removeFriend(player, payload.friend());
                case ACCEPT -> FriendsService.answer(player, payload.friend(), true);
                case DECLINE -> FriendsService.answer(player, payload.friend(), false);
                case CANCEL -> FriendsService.cancel(player, payload.friend());
            }
        });
    }
}
