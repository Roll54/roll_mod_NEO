package com.roll_54.roll_mod.minestar.letters;

import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.network.packet.SyncLettersPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Who has the hub open and therefore holds a letter list that has to stay current. */
public final class LetterViewers {

    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

    private LetterViewers() {}

    public static void add(ServerPlayer player) {
        VIEWERS.add(player.getUUID());
        syncTo(player);
    }

    public static void remove(UUID player) {
        VIEWERS.remove(player);
    }

    public static void resync(MinecraftServer server) {
        for (UUID id : VIEWERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) syncTo(player);
        }
    }

    /** Only the acceptance counts moved, and only the staff list shows those. */
    public static void resyncModerators(MinecraftServer server) {
        for (UUID id : VIEWERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && ModerationPermissions.canWriteLetters(player)) syncTo(player);
        }
    }

    public static void syncTo(ServerPlayer player) {
        // Staff get every letter, due and expired too — it is their list to manage. The reader tab
        // filters those back out on the client, by the same visible() test. Nobody, staff included,
        // is sent a personal letter addressed to someone else.
        List<Letter> letters = ModerationPermissions.canWriteLetters(player)
                ? LetterStore.all(player.server)
                : LetterStore.visible(player.server);
        List<LetterView> views = letters.stream()
                .filter(letter -> letter.isFor(player.getUUID()))
                .map(letter -> LetterView.of(letter, player.getUUID()))
                .toList();
        PacketDistributor.sendToPlayer(player, new SyncLettersPacket(views));
    }
}
