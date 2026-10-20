package com.roll_54.roll_mod.minestar.friends;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.ftb.TeamsFacade;
import com.roll_54.roll_mod.network.packet.SyncFriendDetailPacket;
import com.roll_54.roll_mod.network.packet.SyncFriendsPacket;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The friends tab's server side: fetches a player's Minestar friends through {@link FriendsProvider},
 * keeps the last list per player, and answers the tab's selections and invites.
 *
 * <p>Every provider call is asynchronous and lands back on the server thread through
 * {@code server.execute} before touching anything — the Minestar client completes its futures on
 * its own executor.
 *
 * <p>The list is also the permission check: a detail or an invite is only ever served for a uuid
 * that is in the asking player's own friends list, so the tab cannot be used to read the profile of
 * anyone at all.
 */
public final class FriendsService {

    public enum Status { LOADING, OK, UNAVAILABLE, ERROR }

    /** A list younger than this is re-sent rather than fetched again when the hub is reopened. */
    private static final long LIST_TTL_MILLIS = 30_000L;
    private static final long DETAIL_TTL_MILLIS = 60_000L;

    private record Fetched<T>(T value, long at) {}

    private static final Map<UUID, Fetched<List<FriendEntry>>> LISTS = new ConcurrentHashMap<>();
    /** Each player's pending requests, from the same fetch as their list. */
    private static final Map<UUID, FriendsSnapshot> REQUESTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Status> STATUS = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Fetched<FriendDetail>> DETAILS = new ConcurrentHashMap<>();
    /** When each player last made a profile fetch that missed the cache — see {@link #select}. */
    private static final Map<UUID, Long> LAST_FETCH = new ConcurrentHashMap<>();
    private static final long SELECT_COOLDOWN_MILLIS = 400L;

    private FriendsService() {}

    /** Fetches the list unless a fresh one is on hand; {@code force} always fetches. */
    public static void refresh(ServerPlayer player, boolean force) {
        UUID id = player.getUUID();
        FriendsProvider provider = FriendsProvider.get();
        if (!provider.available()) {
            STATUS.put(id, Status.UNAVAILABLE);
            syncTo(player);
            return;
        }
        Fetched<List<FriendEntry>> cached = LISTS.get(id);
        if (!force && cached != null && System.currentTimeMillis() - cached.at() < LIST_TTL_MILLIS) {
            syncTo(player);
            return;
        }
        if (!LOADING.add(id)) return;
        if (cached == null) STATUS.put(id, Status.LOADING);
        syncTo(player);

        MinecraftServer server = player.server;
        provider.snapshot(id).whenComplete((snapshot, error) -> server.execute(() -> {
            LOADING.remove(id);
            if (error != null) {
                RollMod.LOGGER.warn("[Friends] could not load friends of {}", player.getGameProfile().getName(), error);
                STATUS.put(id, Status.ERROR);
            } else {
                LISTS.put(id, new Fetched<>(List.copyOf(snapshot.friends()), System.currentTimeMillis()));
                REQUESTS.put(id, snapshot);
                STATUS.put(id, Status.OK);
            }
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) syncTo(online);
        }));
    }

    /**
     * Runs {@code action} on the server thread with the player's friends — the cached list when it is
     * fresh, otherwise after fetching it. {@code null} when Minestar is unavailable or the fetch
     * failed. For features that need someone's friends without the hub open, like friends chat.
     */
    public static void withFriends(ServerPlayer player, java.util.function.Consumer<List<FriendEntry>> action) {
        UUID id = player.getUUID();
        Fetched<List<FriendEntry>> cached = LISTS.get(id);
        if (cached != null && System.currentTimeMillis() - cached.at() < LIST_TTL_MILLIS) {
            action.accept(cached.value());
            return;
        }
        FriendsProvider provider = FriendsProvider.get();
        if (!provider.available()) {
            action.accept(null);
            return;
        }
        MinecraftServer server = player.server;
        provider.snapshot(id).whenComplete((snapshot, error) -> server.execute(() -> {
            if (error != null) {
                RollMod.LOGGER.warn("[Friends] could not load friends of {}", player.getGameProfile().getName(), error);
                action.accept(null);
                return;
            }
            LISTS.put(id, new Fetched<>(List.copyOf(snapshot.friends()), System.currentTimeMillis()));
            REQUESTS.put(id, snapshot);
            STATUS.put(id, Status.OK);
            action.accept(snapshot.friends());
        }));
    }

    /** Sends the list with this server's flags worked out now — who is here, who is in the party. */
    public static void syncTo(ServerPlayer player) {
        Fetched<List<FriendEntry>> fetched = LISTS.get(player.getUUID());
        List<FriendEntry> entries = new ArrayList<>();
        if (fetched != null) {
            MinecraftServer server = player.server;
            List<UUID> ids = new ArrayList<>(fetched.value().size());
            for (FriendEntry entry : fetched.value()) {
                boolean here = server.getPlayerList().getPlayer(entry.id()) != null;
                Property skin = SkinLookup.get(server, entry.id());
                entries.add(entry.withServerView(here, TeamsFacade.get().sameParty(player, entry.id()),
                        skin == null ? "" : skin.value(),
                        skin == null || skin.signature() == null ? "" : skin.signature()));
                ids.add(entry.id());
            }
            // Faces not known yet are fetched in the background; the list is sent again, once, when
            // they land. Nothing to fetch means no second send, so this cannot loop.
            UUID viewer = player.getUUID();
            SkinLookup.prefetch(server, ids, () -> {
                ServerPlayer online = server.getPlayerList().getPlayer(viewer);
                if (online != null) syncTo(online);
            });
        }
        Status status = STATUS.getOrDefault(player.getUUID(), Status.LOADING);
        FriendsSnapshot requests = REQUESTS.getOrDefault(player.getUUID(), FriendsSnapshot.EMPTY);
        // Requests from players this one ignores are not shown; Minestar still holds them.
        List<FriendRequest> incoming = requests.incoming().stream()
                .filter(r -> !com.roll_54.roll_mod.minestar.interactions.InteractionSettings.ignores(player.getUUID(), r.id()))
                .toList();
        PacketDistributor.sendToPlayer(player, new SyncFriendsPacket(status, entries,
                incoming, requests.outgoing()));
    }

    /** The player picked someone in the list — a friend, or a player online here: send their profile. */
    public static void select(ServerPlayer player, UUID friend) {
        // A cached detail costs nothing; a miss is a Minestar API call, so those are rate-limited.
        long now = System.currentTimeMillis();
        Fetched<FriendDetail> fresh = DETAILS.get(friend);
        boolean cachedHit = fresh != null && now - fresh.at() < DETAIL_TTL_MILLIS;
        if (!cachedHit) {
            Long last = LAST_FETCH.get(player.getUUID());
            if (last != null && now - last < SELECT_COOLDOWN_MILLIS) return;
            LAST_FETCH.put(player.getUUID(), now);
        }
        if (known(player, friend) == null) return;

        Fetched<FriendDetail> cached = DETAILS.get(friend);
        if (cached != null && System.currentTimeMillis() - cached.at() < DETAIL_TTL_MILLIS) {
            PacketDistributor.sendToPlayer(player, SyncFriendDetailPacket.of(cached.value()));
            return;
        }

        UUID id = player.getUUID();
        MinecraftServer server = player.server;
        FriendsProvider.get().detail(friend).whenComplete((detail, error) -> server.execute(() -> {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (error != null) {
                RollMod.LOGGER.warn("[Friends] could not load the profile of {}", friend, error);
                if (online != null) PacketDistributor.sendToPlayer(online, SyncFriendDetailPacket.failed(friend));
                return;
            }
            DETAILS.put(friend, new Fetched<>(detail, System.currentTimeMillis()));
            if (online != null) PacketDistributor.sendToPlayer(online, SyncFriendDetailPacket.of(detail));
        }));
    }

    /**
     * {@code id}'s Minestar profile, from the same cache {@link #select} keeps, fetched on a miss.
     * For callers that only want the profile — the Hueri statue's player tags. Completes on any thread.
     */
    public static java.util.concurrent.CompletableFuture<FriendDetail> detail(UUID id) {
        Fetched<FriendDetail> cached = DETAILS.get(id);
        if (cached != null && System.currentTimeMillis() - cached.at() < DETAIL_TTL_MILLIS) {
            return java.util.concurrent.CompletableFuture.completedFuture(cached.value());
        }
        return FriendsProvider.get().detail(id).thenApply(detail -> {
            DETAILS.put(id, new Fetched<>(detail, System.currentTimeMillis()));
            return detail;
        });
    }

    /**
     * Invites a friend who is on this server into the player's FTB party, creating the party first
     * if they have none. Done through FTB Teams' own commands, run as the player, so its permission
     * checks and its messages — including the clickable invite the friend receives — are exactly the
     * ones {@code /ftbteams party invite} gives.
     */
    public static void invite(ServerPlayer player, UUID friend) {
        FriendEntry entry = known(player, friend);
        if (entry == null) return;
        if (!ModList.get().isLoaded("ftbteams")) {
            player.sendSystemMessage(Component.translatable("gui.roll_mod.hub.friends.invite.noTeams"));
            return;
        }
        ServerPlayer target = player.server.getPlayerList().getPlayer(friend);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gui.roll_mod.hub.friends.invite.offline", entry.name()));
            return;
        }
        if (TeamsFacade.get().sameParty(player, friend)) {
            player.sendSystemMessage(Component.translatable("gui.roll_mod.hub.friends.invite.already", entry.name()));
            return;
        }
        var commands = player.server.getCommands();
        if (TeamsFacade.get().party(player).isEmpty()) {
            commands.performPrefixedCommand(player.createCommandSourceStack(), "ftbteams party create");
        }
        commands.performPrefixedCommand(player.createCommandSourceStack(),
                "ftbteams party invite " + target.getGameProfile().getName());
    }

    /**
     * Sends a Minestar friend request to a player on this server. The list is reloaded afterwards
     * for both of them, so an instantly accepted request shows up on both sides.
     */
    public static void addFriend(ServerPlayer player, UUID targetId) {
        ServerPlayer target = player.server.getPlayerList().getPlayer(targetId);
        if (target == null || target == player || find(player, targetId) != null) return;
        String name = target.getGameProfile().getName();
        MinecraftServer server = player.server;
        UUID id = player.getUUID();
        FriendsProvider.get().addFriend(id, targetId).whenComplete((nowFriends, error) -> server.execute(() -> {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online == null) return;
            if (error != null) {
                RollMod.LOGGER.warn("[Friends] {} could not add {}", online.getGameProfile().getName(), name, error);
                online.sendSystemMessage(failure("add", error, name));
                return;
            }
            online.sendSystemMessage(Component.translatable(nowFriends
                    ? "gui.roll_mod.hub.friends.add.accepted" : "gui.roll_mod.hub.friends.add.sent", name));
            ServerPlayer other = server.getPlayerList().getPlayer(targetId);
            if (other != null && !nowFriends) {
                other.sendSystemMessage(Component.translatable("gui.roll_mod.hub.friends.add.incoming",
                        online.getGameProfile().getName()));
            }
            refresh(online, true);
            if (other != null) refresh(other, true);
        }));
    }

    /** Removes a friend from the player's Minestar friends; both lists are reloaded afterwards. */
    public static void removeFriend(ServerPlayer player, UUID friendId) {
        FriendEntry entry = find(player, friendId);
        if (entry == null) return;
        MinecraftServer server = player.server;
        UUID id = player.getUUID();
        FriendsProvider.get().removeFriend(id, friendId).whenComplete((ignored, error) -> server.execute(() -> {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online == null) return;
            if (error != null) {
                RollMod.LOGGER.warn("[Friends] {} could not remove {}", online.getGameProfile().getName(),
                        entry.name(), error);
                online.sendSystemMessage(failure("remove", error, entry.name()));
                return;
            }
            online.sendSystemMessage(Component.translatable("gui.roll_mod.hub.friends.remove.done", entry.name()));
            refresh(online, true);
            ServerPlayer other = server.getPlayerList().getPlayer(friendId);
            if (other != null) refresh(other, true);
        }));
    }

    /**
     * Answers a request someone sent the player: accepting sends one back, which Minestar treats as
     * the acceptance; declining removes it. Only for a request that really is in the player's
     * incoming list.
     */
    public static void answer(ServerPlayer player, UUID from, boolean accept) {
        FriendRequest request = request(REQUESTS.getOrDefault(player.getUUID(), FriendsSnapshot.EMPTY).incoming(), from);
        if (request == null) return;
        FriendsProvider provider = FriendsProvider.get();
        CompletableFuture<?> call = accept ? provider.addFriend(player.getUUID(), from)
                : provider.removeFriend(player.getUUID(), from);
        afterRequestChange(player, from, request.name(), call, accept ? "accept" : "decline");
    }

    /** Withdraws a request the player sent and nobody has answered yet. */
    public static void cancel(ServerPlayer player, UUID to) {
        FriendRequest request = request(REQUESTS.getOrDefault(player.getUUID(), FriendsSnapshot.EMPTY).outgoing(), to);
        if (request == null) return;
        afterRequestChange(player, to, request.name(),
                FriendsProvider.get().removeFriend(player.getUUID(), to), "cancel");
    }

    /** Tells the player how it went and reloads both sides' lists. */
    private static void afterRequestChange(ServerPlayer player, UUID other, String name,
                                           CompletableFuture<?> call, String action) {
        MinecraftServer server = player.server;
        UUID id = player.getUUID();
        call.whenComplete((ignored, error) -> server.execute(() -> {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online == null) return;
            if (error != null) {
                RollMod.LOGGER.warn("[Friends] {} could not {} the request of {}",
                        online.getGameProfile().getName(), action, name, error);
                online.sendSystemMessage(Component.translatable("gui.roll_mod.hub.friends.request." + action + ".error", name));
                return;
            }
            online.sendSystemMessage(Component.translatable("gui.roll_mod.hub.friends.request." + action + ".done", name));
            refresh(online, true);
            ServerPlayer otherOnline = server.getPlayerList().getPlayer(other);
            if (otherOnline != null) {
                if ("accept".equals(action)) {
                    otherOnline.sendSystemMessage(Component.translatable(
                            "gui.roll_mod.hub.friends.request.accepted", online.getGameProfile().getName()));
                }
                refresh(otherOnline, true);
            }
        }));
    }

    private static FriendRequest request(List<FriendRequest> requests, UUID id) {
        for (FriendRequest request : requests) {
            if (request.id().equals(id)) return request;
        }
        return null;
    }

    /**
     * The chat line for a failed add or remove. Minestar fails with the HTTP status in the message
     * (see its usage notes), so the known ones get a message of their own: 400 adding yourself, 404
     * an unknown player, 409 already friends or already asked, 429 too many pending requests.
     */
    private static Component failure(String action, Throwable error, String name) {
        String text = String.valueOf(error.getMessage()) + " " + (error.getCause() == null ? "" : error.getCause().getMessage());
        for (String code : new String[] {"404", "409", "429"}) {
            if (text.contains(code)) {
                return Component.translatable("gui.roll_mod.hub.friends." + action + ".error." + code, name);
            }
        }
        return Component.translatable("gui.roll_mod.hub.friends." + action + ".error", name);
    }

    public static void forget(UUID player) {
        LISTS.remove(player);
        LAST_FETCH.remove(player);
        REQUESTS.remove(player);
        STATUS.remove(player);
        LOADING.remove(player);
    }

    /**
     * A friend from the player's list, or — for the "online players" view — anyone else on this
     * server, as a bare entry; the viewer themself; or anyone a cached leaderboard lists. Null for
     * anyone else: the tab cannot read arbitrary profiles.
     */
    private static FriendEntry known(ServerPlayer player, UUID id) {
        FriendEntry friend = find(player, id);
        if (friend != null) return friend;
        // Someone with a request waiting either way: their profile is what the answer is about.
        FriendsSnapshot requests = REQUESTS.getOrDefault(player.getUUID(), FriendsSnapshot.EMPTY);
        FriendRequest request = request(requests.incoming(), id);
        if (request == null) request = request(requests.outgoing(), id);
        if (request != null) {
            boolean here = player.server.getPlayerList().getPlayer(id) != null;
            return new FriendEntry(id, request.name(), here, -1L, List.of());
        }
        ServerPlayer online = player.server.getPlayerList().getPlayer(id);
        // The viewer themself: the home tab shows their own Minestar level and group.
        if (online == player) return new FriendEntry(id, player.getGameProfile().getName(), true, -1L, List.of());
        if (online != null) return new FriendEntry(id, online.getGameProfile().getName(), true, -1L, List.of());
        // Someone on a public leaderboard: a row there opens their profile.
        String ranked = com.roll_54.roll_mod.minestar.leaderboard.LeaderboardService.rankedName(id);
        if (ranked != null) return new FriendEntry(id, ranked, false, -1L, List.of());
        return null;
    }

    private static FriendEntry find(ServerPlayer player, UUID friend) {
        Fetched<List<FriendEntry>> fetched = LISTS.get(player.getUUID());
        if (fetched == null) return null;
        for (FriendEntry entry : fetched.value()) {
            if (entry.id().equals(friend)) return entry;
        }
        return null;
    }
}
