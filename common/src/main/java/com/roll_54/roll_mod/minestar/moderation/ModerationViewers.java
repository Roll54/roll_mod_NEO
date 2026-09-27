package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod.network.packet.SyncModerationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who has the hub open, and what the moderation tab shows them.
 *
 * <p>Everyone watching gets a payload; everyone who may not moderate gets {@link
 * SyncModerationPacket#DENIED}, and that emptiness is what hides the tab — the tree itself is built
 * the same for everybody, see {@code HubUI}.
 *
 * <p>Changes are pushed by a dirty flag rather than by call sites. Every store raises it when it
 * saves or reloads, so a mute from {@code /mute}, a ban from the GUI, a hand-edited file picked up
 * by {@code /rollmod reload} and a login all reach open hubs without any of them having to remember
 * to. {@link #tick} flushes it at most once a second.
 */
public final class ModerationViewers {

    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();
    private static volatile boolean dirty;

    private ModerationViewers() {}

    public static void add(ServerPlayer player) {
        VIEWERS.add(player.getUUID());
        syncTo(player);
    }

    public static void remove(UUID player) {
        VIEWERS.remove(player);
    }

    /** Something a moderator can see has changed; open hubs catch up on the next flush. */
    public static void markDirty() {
        dirty = true;
    }

    /** Called every server tick; only does work once a second, and only when something changed. */
    public static void tick(MinecraftServer server) {
        if (!dirty || server.getTickCount() % 20 != 0) return;
        dirty = false;
        resync(server);
    }

    public static void resync(MinecraftServer server) {
        if (VIEWERS.isEmpty()) return;
        SyncModerationPacket full = null;
        for (UUID id : VIEWERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            if (!ModerationPermissions.canModerate(player)) {
                PacketDistributor.sendToPlayer(player, SyncModerationPacket.DENIED);
                continue;
            }
            // Built once and shared: only the two permission flags differ between moderators.
            if (full == null) full = snapshot(server);
            PacketDistributor.sendToPlayer(player, full.forViewer(
                    ModerationPermissions.canBan(player),
                    ModerationPermissions.canWriteLetters(player)));
        }
    }

    public static void syncTo(ServerPlayer player) {
        if (!ModerationPermissions.canModerate(player)) {
            PacketDistributor.sendToPlayer(player, SyncModerationPacket.DENIED);
            return;
        }
        PacketDistributor.sendToPlayer(player, snapshot(player.server).forViewer(
                ModerationPermissions.canBan(player),
                ModerationPermissions.canWriteLetters(player)));
    }

    /**
     * Everyone online, then everyone banned who is not: the two groups a moderator acts on. An
     * offline player with warnings but no ban is left out — there is nothing to do to them from here
     * that the commands do not already cover, and listing every warned player ever would bury the
     * people actually on the server.
     */
    private static SyncModerationPacket snapshot(MinecraftServer server) {
        long now = System.currentTimeMillis();
        List<ModerationRow> rows = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();

        List<ServerPlayer> online = new ArrayList<>(server.getPlayerList().getPlayers());
        online.sort(Comparator.comparing(p -> p.getGameProfile().getName(),
                String.CASE_INSENSITIVE_ORDER));
        for (ServerPlayer p : online) {
            seen.add(p.getUUID());
            rows.add(row(p.getUUID(), p.getGameProfile().getName(), true, now));
        }

        List<Map.Entry<UUID, BanStore.Ban>> bans = new ArrayList<>(BanStore.active().entrySet());
        bans.sort(Comparator.comparing(e -> e.getValue().name(), String.CASE_INSENSITIVE_ORDER));
        for (Map.Entry<UUID, BanStore.Ban> entry : bans) {
            if (!seen.add(entry.getKey())) continue;
            rows.add(row(entry.getKey(), entry.getValue().name(), false, now));
        }

        return new SyncModerationPacket(true, false, false, WhitelistStore.enabled(),
                WhitelistStore.kickMessage(), rows);
    }

    private static ModerationRow row(UUID id, String name, boolean online, long now) {
        MuteStore.Mute mute = MuteStore.mute(id);
        BanStore.Ban ban = BanStore.ban(id);
        return new ModerationRow(id, name, online, WarnStore.count(id),
                mute == null ? ModerationRow.NONE : ModerationRow.wire(mute.remaining(now)),
                ban == null ? ModerationRow.NONE : ModerationRow.wire(ban.remaining(now)),
                // The rule travels as its id; the client words it in its own language.
                ban == null ? "" : ban.rule(),
                ban == null ? "" : ban.reason());
    }
}
