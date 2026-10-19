package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod.network.packet.SyncModerationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
            // Built once and shared: only the permission flags differ between moderators.
            if (full == null) full = snapshot(server);
            PacketDistributor.sendToPlayer(player, full.forViewer(
                    ModerationPermissions.canBan(player),
                    ModerationPermissions.canWriteLetters(player),
                ModerationPermissions.canUseDeathlog(player),
                ModerationPermissions.canInvSee(player),
                ModerationPermissions.canInvEdit(player)));
        }
    }

    public static void syncTo(ServerPlayer player) {
        if (!ModerationPermissions.canModerate(player)) {
            PacketDistributor.sendToPlayer(player, SyncModerationPacket.DENIED);
            return;
        }
        PacketDistributor.sendToPlayer(player, snapshot(player.server).forViewer(
                ModerationPermissions.canBan(player),
                ModerationPermissions.canWriteLetters(player),
                ModerationPermissions.canUseDeathlog(player),
                ModerationPermissions.canInvSee(player),
                ModerationPermissions.canInvEdit(player)));
    }

    /**
     * Everyone online, then everyone banned who is not, then every other offline player with a
     * record (warnings or a running mute), then every other player who has saved data in this world.
     * The tab's presence and "punished" filters sort them apart, so the offline rows do not bury the
     * people actually on the server. Clean offline players come last because the packet caps the
     * list ({@code SyncModerationPacket.MAX_ROWS}). On a huge server, they are the ones cut.
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

        Set<UUID> punished = new HashSet<>(WarnStore.counts().keySet());
        punished.addAll(MuteStore.muted());
        punished.removeAll(seen);
        List<ModerationRow> offline = new ArrayList<>();
        for (UUID id : punished) {
            offline.add(row(id, offlineName(server, id), false, now));
        }
        offline.sort(Comparator.comparing(ModerationRow::name, String.CASE_INSENSITIVE_ORDER));
        rows.addAll(offline);
        seen.addAll(punished);

        // Everyone else who has played here: one playerdata file each. The same set "Offline
        // inventory" can open.
        List<ModerationRow> known = new ArrayList<>();
        for (UUID id : savedPlayers(server)) {
            if (!seen.add(id)) continue;
            known.add(row(id, offlineName(server, id), false, now));
        }
        known.sort(Comparator.comparing(ModerationRow::name, String.CASE_INSENSITIVE_ORDER));
        rows.addAll(known);

        return new SyncModerationPacket(true, false, false, false, false, false, WhitelistStore.enabled(),
                WhitelistStore.kickMessage(), rows);
    }

    /** Every player with a {@code playerdata/<uuid>.dat} in this world. */
    private static List<UUID> savedPlayers(MinecraftServer server) {
        List<UUID> out = new ArrayList<>();
        Path dir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if (!Files.isDirectory(dir)) return out;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.dat")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                try {
                    out.add(UUID.fromString(name.substring(0, name.length() - 4)));
                } catch (IllegalArgumentException ignored) {
                    // Not a player file.
                }
            }
        } catch (IOException e) {
            RollMod.LOGGER.error("[Moderation] could not list saved players", e);
        }
        return out;
    }

    /** The name a warn was recorded under, else the server's profile cache, else the UUID. */
    private static String offlineName(MinecraftServer server, UUID id) {
        String name = WarnStore.name(id);
        if (!name.isBlank()) return name;
        if (server.getProfileCache() != null) {
            var cached = server.getProfileCache().get(id);
            if (cached.isPresent()) return cached.get().getName();
        }
        return id.toString();
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
