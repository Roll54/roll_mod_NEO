package com.roll_54.roll_mod.minestar.friends;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Where the friends tab gets its Minestar data.
 *
 * <p>The Minestar client mod is only on the dedicated-server jar's classpath ({@code :server}), and
 * the hub lives in {@code :common}, so the server module registers the real implementation at
 * construction — {@code MinestarFriendsProvider} — and everything else sees {@link #NONE}: the dev
 * client, single player, any server without the server jar. The tab then says Minestar is
 * unavailable instead of failing.
 *
 * <p>Futures may complete on any thread; {@link FriendsService} hops back to the server thread.
 */
public interface FriendsProvider {

    /**
     * The player's accepted friends, names and flags as Minestar reports them, and the friend
     * requests still waiting in either direction.
     */
    CompletableFuture<FriendsSnapshot> snapshot(UUID player);

    /** One player's public profile — status, group, prime, level, rewards. */
    CompletableFuture<FriendDetail> detail(UUID player);

    /**
     * Sends {@code player}'s friend request to {@code target} — accepted on the spot if {@code target}
     * had already sent one. Completes with {@code true} when the two are now friends, {@code false}
     * when the request is waiting for an answer.
     */
    CompletableFuture<Boolean> addFriend(UUID player, UUID target);

    /** Removes a friend, or cancels a pending request either way. */
    CompletableFuture<Void> removeFriend(UUID player, UUID target);

    default boolean available() {
        return true;
    }

    FriendsProvider NONE = new FriendsProvider() {
        @Override
        public CompletableFuture<FriendsSnapshot> snapshot(UUID player) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minestar is not installed"));
        }

        @Override
        public CompletableFuture<FriendDetail> detail(UUID player) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minestar is not installed"));
        }

        @Override
        public CompletableFuture<Boolean> addFriend(UUID player, UUID target) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minestar is not installed"));
        }

        @Override
        public CompletableFuture<Void> removeFriend(UUID player, UUID target) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minestar is not installed"));
        }

        @Override
        public boolean available() {
            return false;
        }
    };

    static FriendsProvider get() {
        return Holder.instance;
    }

    /** Called once by the server module; a later call replaces it. */
    static void set(FriendsProvider provider) {
        Holder.instance = provider == null ? NONE : provider;
    }

    final class Holder {
        private static volatile FriendsProvider instance = NONE;

        private Holder() {}
    }
}
