package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod.minestar.friends.FriendDetail;
import com.roll_54.roll_mod.minestar.friends.FriendEntry;
import com.roll_54.roll_mod.minestar.friends.FriendRequest;
import com.roll_54.roll_mod.minestar.friends.FriendsSnapshot;
import com.roll_54.roll_mod.minestar.friends.FriendsProvider;
import ua.com.minestar.model.FriendRequestStatus;
import ua.com.minestar.model.PackSummary;
import ua.com.minestar.model.PrimeStatus;
import ua.com.minestar.model.UserFriend;
import ua.com.minestar.model.UserFriendRequest;
import ua.com.minestar.model.UserGroup;
import ua.com.minestar.model.UserProfile;
import ua.com.minestar.model.UserReward;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static ua.com.minestar.Minestar.minestar;

/**
 * The friends tab's data, from the Minestar API: a player's {@code profile.friends()} for the list,
 * and a friend's own profile for the detail panel. Registered by {@code RollModServer}, so it only
 * exists on the dedicated server — see {@link FriendsProvider}.
 *
 * <p>Reward images are dropped here on purpose: the tab shows reward names only.
 */
public final class MinestarFriendsProvider implements FriendsProvider {

    @Override
    public CompletableFuture<FriendsSnapshot> snapshot(UUID player) {
        return minestar.getUserProfileByProfileUuid(player).completableFuture()
                .thenApply(profile -> {
                    List<FriendEntry> entries = new ArrayList<>(profile.friends().size());
                    for (UserFriend friend : profile.friends()) {
                        entries.add(entry(friend));
                    }
                    return new FriendsSnapshot(List.copyOf(entries), requests(profile.incomingRequests()),
                            requests(profile.outgoingRequests()));
                });
    }

    private static List<FriendRequest> requests(List<UserFriendRequest> requests) {
        List<FriendRequest> out = new ArrayList<>(requests.size());
        for (UserFriendRequest request : requests) {
            out.add(new FriendRequest(request.uuid(), FriendDetail.clip(request.nickname(), 64),
                    request.sentAt() == null ? 0L : request.sentAt().toEpochMilli()));
        }
        return List.copyOf(out);
    }

    @Override
    public CompletableFuture<FriendDetail> detail(UUID player) {
        return minestar.getUserProfileByProfileUuid(player).completableFuture()
                .thenApply(MinestarFriendsProvider::detail);
    }

    @Override
    public CompletableFuture<Boolean> addFriend(UUID player, UUID target) {
        return minestar.addUserFriend(player, target).completableFuture()
                .thenApply(request -> request.status() == FriendRequestStatus.ACCEPTED);
    }

    @Override
    public CompletableFuture<Void> removeFriend(UUID player, UUID target) {
        return minestar.removeUserFriend(player, target).completableFuture().thenApply(unit -> null);
    }

    private static FriendEntry entry(UserFriend friend) {
        List<String> playing = new ArrayList<>();
        for (PackSummary pack : friend.playing()) {
            playing.add(FriendDetail.clip(pack.name(), 128));
        }
        return new FriendEntry(friend.uuid(), FriendDetail.clip(friend.nickname(), 64), friend.online(),
                friend.lastSeenAt().map(Instant::toEpochMilli).orElse(-1L), List.copyOf(playing));
    }

    private static FriendDetail detail(UserProfile profile) {
        List<FriendDetail.Reward> rewards = new ArrayList<>(profile.rewards().size());
        for (UserReward reward : profile.rewards()) {
            rewards.add(new FriendDetail.Reward(FriendDetail.clip(reward.title(), 256),
                    FriendDetail.clip(reward.description(), 1024),
                    reward.rarity() == null ? 0 : reward.rarity().ordinal()));
        }
        UserGroup group = profile.group().orElse(null);
        return new FriendDetail(profile.uuid(), FriendDetail.clip(profile.nickname(), 64),
                group == null ? "" : FriendDetail.clip(group.name(), 64),
                group == null ? -1 : color(group.color()),
                profile.primeStatus().map(PrimeStatus::expiresAt).map(Instant::toEpochMilli).orElse(-1L),
                profile.xp().level(), Math.max(0, profile.xp().currentLevelXp()),
                Math.max(0, profile.xp().xpForCurrentLevel()), Math.max(0, profile.xp().xpToNextLevel()),
                profile.playtime().toHours(), List.copyOf(rewards));
    }

    /** {@code "#ffffff"} → 0xFFFFFF; {@code -1} for anything else. */
    private static int color(String hex) {
        if (hex == null) return -1;
        String digits = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return digits.length() == 6 ? Integer.parseInt(digits, 16) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
