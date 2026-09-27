package com.roll_54.roll_mod.minestar.moderation;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

/**
 * One player as the moderation tab lists them.
 *
 * <p>Both punishments travel as <em>remaining</em> millis, not deadlines, so the client's clock never
 * has to agree with the server's: {@code -1} none, {@code 0} no end, otherwise how long is left.
 * {@code ClientModerationCache} turns them into local deadlines on arrival, which is what lets a
 * countdown run on screen without a packet per second.
 */
public record ModerationRow(UUID id, String name, boolean online, int warns,
                            long muteRemaining, long banRemaining, String banRule, String banReason) {

    public static final long NONE = -1L;
    public static final long FOREVER = 0L;

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeBoolean(online);
        buf.writeVarInt(warns);
        buf.writeVarLong(muteRemaining + 1L);   // +1 keeps the -1 sentinel non-negative
        buf.writeVarLong(banRemaining + 1L);
        buf.writeUtf(banRule, 32);
        buf.writeUtf(banReason, 256);
    }

    public static ModerationRow decode(RegistryFriendlyByteBuf buf) {
        return new ModerationRow(buf.readUUID(), buf.readUtf(64), buf.readBoolean(), buf.readVarInt(),
                buf.readVarLong() - 1L, buf.readVarLong() - 1L, buf.readUtf(32), buf.readUtf(256));
    }

    public boolean muted() {
        return muteRemaining != NONE;
    }

    public boolean banned() {
        return banRemaining != NONE;
    }

    /** Store remaining ({@code -1} forever) → wire remaining ({@code 0} forever, {@code -1} none). */
    static long wire(long storeRemaining) {
        return storeRemaining < 0L ? FOREVER : Math.max(1L, storeRemaining);
    }
}
