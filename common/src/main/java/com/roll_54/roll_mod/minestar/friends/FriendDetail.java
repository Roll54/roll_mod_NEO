package com.roll_54.roll_mod.minestar.friends;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The selected friend's Minestar profile, as the right-hand panel of the friends tab shows it.
 * Rewards travel as names only — the API's image URLs are dropped on the server.
 *
 * @param groupName        their Minestar group, empty when none
 * @param groupColor       the group's colour as 0xRRGGBB, {@code -1} when none or unparseable
 * @param primeUntilMillis when their prime runs out, {@code -1} without prime
 * @param levelXp          XP earned inside the current level, out of {@code levelXpNeeded}
 * @param xpToNext         XP still missing for the next level
 */
public record FriendDetail(UUID id, String name, String groupName, int groupColor, long primeUntilMillis,
                           int level, long levelXp, long levelXpNeeded, long xpToNext,
                           long playtimeHours, List<Reward> rewards) {

    private static final int MAX_REWARDS = 256;

    /** One earned Minestar reward. {@code rarity} is the API's ordinal: 0 common … 3 legendary. */
    public record Reward(String title, String description, int rarity) {}

    public boolean prime() {
        return primeUntilMillis >= 0;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeUtf(groupName, 64);
        buf.writeInt(groupColor);
        buf.writeLong(primeUntilMillis);
        buf.writeVarInt(level);
        buf.writeVarLong(levelXp);
        buf.writeVarLong(levelXpNeeded);
        buf.writeVarLong(xpToNext);
        buf.writeVarLong(playtimeHours);
        int count = Math.min(MAX_REWARDS, rewards.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            Reward reward = rewards.get(i);
            buf.writeUtf(reward.title(), 256);
            buf.writeUtf(reward.description(), 1024);
            buf.writeVarInt(reward.rarity());
        }
    }

    public static FriendDetail decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String name = buf.readUtf(64);
        String groupName = buf.readUtf(64);
        int groupColor = buf.readInt();
        long primeUntil = buf.readLong();
        int level = buf.readVarInt();
        long levelXp = buf.readVarLong();
        long levelXpNeeded = buf.readVarLong();
        long xpToNext = buf.readVarLong();
        long hours = buf.readVarLong();
        int count = Math.min(MAX_REWARDS, buf.readVarInt());
        List<Reward> rewards = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rewards.add(new Reward(buf.readUtf(256), buf.readUtf(1024), buf.readVarInt()));
        }
        return new FriendDetail(id, name, groupName, groupColor, primeUntil, level, levelXp, levelXpNeeded, xpToNext,
                hours, List.copyOf(rewards));
    }

    /** Text longer than the wire allows is cut rather than failing the whole packet. */
    public static String clip(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
