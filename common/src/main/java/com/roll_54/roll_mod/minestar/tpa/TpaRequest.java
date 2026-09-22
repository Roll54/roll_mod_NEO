package com.roll_54.roll_mod.minestar.tpa;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

/**
 * One pending teleport request.
 *
 * <p>{@link Kind#TO} is "let me come to you"; {@link Kind#HERE} is "come to me". Which of the two
 * it is decides who moves when it is accepted, and nothing else.
 */
public record TpaRequest(UUID id, UUID from, String fromName, UUID to, String toName, Kind kind,
                         long expiresAt) {

    public enum Kind { TO, HERE }

    public long remainingMillis(long now) {
        return Math.max(0L, expiresAt - now);
    }

    public boolean expired(long now) {
        return now >= expiresAt;
    }

    /** Who ends up somewhere else when this is accepted. */
    public UUID mover() {
        return kind == Kind.TO ? from : to;
    }

    /** Whose position the mover is sent to. */
    public UUID anchor() {
        return kind == Kind.TO ? to : from;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUUID(from);
        buf.writeUtf(fromName, 16);
        buf.writeUUID(to);
        buf.writeUtf(toName, 16);
        buf.writeVarInt(kind.ordinal());
        buf.writeVarLong(expiresAt);
    }

    public static TpaRequest decode(RegistryFriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        UUID from = buf.readUUID();
        String fromName = buf.readUtf(16);
        UUID to = buf.readUUID();
        String toName = buf.readUtf(16);
        // Modulo rather than a bounds check: a crafted ordinal must not be able to throw here.
        Kind kind = Kind.values()[Math.floorMod(buf.readVarInt(), Kind.values().length)];
        return new TpaRequest(id, from, fromName, to, toName, kind, buf.readVarLong());
    }
}
