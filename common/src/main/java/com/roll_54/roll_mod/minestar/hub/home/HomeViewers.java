package com.roll_54.roll_mod.minestar.hub.home;

import com.roll_54.roll_mod.compat.ftb.HomesFacade;
import com.roll_54.roll_mod.network.packet.SyncHomesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players with the hub open, and therefore holding a home list that has to stay current. Pushed on
 * open and again whenever a home is created, moved, deleted, shared or answered.
 *
 * <p>Differs from {@code WarpViewers} in one way that matters: warps are one global list broadcast
 * unchanged to everyone, but a home snapshot is built <em>per recipient</em> — it contains only the
 * homes that viewer owns or was invited to, each projected through their own relation to it. So
 * {@link #resync} has to build a payload per viewer and must not be "optimised" into one broadcast.
 */
public final class HomeViewers {

    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

    private HomeViewers() {}

    public static void add(ServerPlayer player) {
        VIEWERS.add(player.getUUID());
        syncTo(player);
    }

    public static void remove(UUID id) {
        VIEWERS.remove(id);
    }

    public static void resync(MinecraftServer server) {
        for (UUID id : VIEWERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) syncTo(player);
        }
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncHomesPacket(
                viewFor(player), HomeService.limitFor(player), KnownPlayers.namesFor(player)));
    }

    /** Everything this player may see, each row carrying what they are to it. */
    private static List<HomeView> viewFor(ServerPlayer player) {
        UUID id = player.getUUID();
        List<HomeView> views = new ArrayList<>();
        for (PlayerHome home : HomeData.get(player.server).visibleTo(id)) {
            HomeView.Relation relation;
            List<HomeView.ShareView> roster = List.of();
            if (home.isOwner(id)) {
                relation = HomeView.Relation.OWNER;
                // Only the owner is told who else is on the list; a guest sees the home, not the
                // other guests.
                List<HomeView.ShareView> mine = new ArrayList<>();
                // activeShares, not shares: an invite that lapsed is gone as far as everything else
                // is concerned, so showing it on the owner's roster would be the one place it lived on.
                for (HomeShare share : home.activeShares()) {
                    mine.add(new HomeView.ShareView(share.playerName(), share.isAccepted()));
                }
                roster = mine;
            } else {
                HomeShare share = home.shareFor(id);
                relation = share != null && share.isAccepted()
                        ? HomeView.Relation.GUEST
                        : HomeView.Relation.INVITED;
            }
            views.add(new HomeView(home.id(), home.owner(), home.ownerName(), home.name(),
                    home.dimension(), home.x(), home.y(), home.z(), home.created(), relation,
                    roster));
        }
        addLegacy(player, views);
        return views;
    }

    /**
     * Appends the player's un-migrated FTB Essentials homes as {@link HomeView.Relation#FTB} rows.
     *
     * <p>These have no {@code PlayerHome} behind them, so they get a synthetic id — derived from the
     * owner and the name rather than random, so the same FTB home keeps the same id across every
     * resync and the tab's selection and signature do not churn every tick. Nothing resolves that id
     * back to anything: {@code MIGRATE} travels by name.
     */
    private static void addLegacy(ServerPlayer player, List<HomeView> views) {
        for (HomesFacade.Legacy legacy : HomesFacade.get().homesOf(player)) {
            views.add(new HomeView(legacyId(player.getUUID(), legacy.name()), player.getUUID(),
                    player.getGameProfile().getName(), legacy.name(), legacy.dimension(),
                    legacy.x(), legacy.y(), legacy.z(), 0L, HomeView.Relation.FTB, List.of()));
        }
    }

    private static UUID legacyId(UUID owner, String name) {
        return UUID.nameUUIDFromBytes(
                ("ftbe:" + owner + ":" + name).getBytes(StandardCharsets.UTF_8));
    }
}
