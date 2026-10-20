package com.roll_54.roll_mod.minestar.friends;

import java.util.List;

/**
 * Everything the friends tab needs from one Minestar profile call: the accepted friends, and the
 * requests still waiting either way.
 */
public record FriendsSnapshot(List<FriendEntry> friends, List<FriendRequest> incoming,
                              List<FriendRequest> outgoing) {

    public static final FriendsSnapshot EMPTY = new FriendsSnapshot(List.of(), List.of(), List.of());
}
