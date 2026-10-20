package com.roll_54.roll_mod.minestar.friends;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * A Minestar friend request still waiting for an answer — one the viewer received (incoming) or
 * sent (outgoing). Minestar has no separate accept call: an incoming request is accepted by sending
 * one back, and declined or cancelled by removing it.
 *
 * @param sentAtMillis when it was sent, epoch millis
 */
public record FriendRequest(UUID id, String name, long sentAtMillis) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeLong(sentAtMillis);
    }

    public static FriendRequest decode(FriendlyByteBuf buf) {
        return new FriendRequest(buf.readUUID(), buf.readUtf(64), buf.readLong());
    }
}
