package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod.minestar.letters.LetterService;
import com.roll_54.roll_mod.network.packet.SyncPlayerStatusPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Tells a player where they stand: warnings, mute, unread letters.
 *
 * <p>Pushed on login and after anything that changes one of those, rather than polled — each is a
 * handful of bytes and changes rarely, and the hub button needs it with no screen open.
 */
public final class ModerationStatus {

    private ModerationStatus() {}

    public static void sendTo(ServerPlayer player) {
        MuteStore.Mute mute = MuteStore.mute(player.getUUID());
        long muteRemaining = mute == null ? -1L : mute.until() == 0L ? 0L
                : Math.max(1L, mute.remaining(System.currentTimeMillis()));
        PacketDistributor.sendToPlayer(player, new SyncPlayerStatusPacket(
                WarnStore.count(player.getUUID()),
                muteRemaining,
                LetterService.unread(player),
                ModerationPermissions.canModerate(player),
                DailyTaskManager.claimableCount(player)));
    }

    /** {@link #sendTo} if they are online; a no-op otherwise, since login sends it anyway. */
    public static void sendTo(MinecraftServer server, UUID player) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) sendTo(online);
    }
}
