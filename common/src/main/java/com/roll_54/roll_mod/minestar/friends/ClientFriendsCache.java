package com.roll_54.roll_mod.minestar.friends;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The client's copy of the friends tab's data, kept current by {@code SyncFriendsPacket} and
 * {@code SyncFriendDetailPacket}. {@link #VERSION} moves on every packet, so the tab only rebuilds
 * when something actually arrived.
 */
public final class ClientFriendsCache {

    public static volatile FriendsService.Status STATUS = FriendsService.Status.LOADING;
    public static volatile List<FriendEntry> ENTRIES = List.of();
    /** Requests waiting for this player's answer, and the ones they sent. */
    public static volatile List<FriendRequest> INCOMING = List.of();
    public static volatile List<FriendRequest> OUTGOING = List.of();

    public static final Map<UUID, FriendDetail> DETAILS = new ConcurrentHashMap<>();
    public static final Set<UUID> FAILED = ConcurrentHashMap.newKeySet();

    public static volatile int VERSION;

    private ClientFriendsCache() {}
}
