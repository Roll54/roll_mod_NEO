package com.roll_54.roll_mod.minestar.leaderboard;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * One line of a board: a player — or, on the quests board, a team — and their value.
 *
 * @param rank  1-based; {@code 0} for "not ranked" on the viewer's own line
 * @param label shown instead of the formatted value when not blank: the chapter board's stage ("HV · 40%")
 */
public record LeaderboardRow(int rank, UUID id, String name, long value, String label) {

    public LeaderboardRow(int rank, UUID id, String name, long value) {
        this(rank, id, name, value, "");
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(rank);
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeVarLong(Math.max(0, value));
        buf.writeUtf(label, 128);
    }

    public static LeaderboardRow decode(FriendlyByteBuf buf) {
        return new LeaderboardRow(buf.readVarInt(), buf.readUUID(), buf.readUtf(64), buf.readVarLong(), buf.readUtf(128));
    }
}
