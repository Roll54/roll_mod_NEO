package com.roll_54.roll_mod.minestar.hub;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The player's rank, as shown on the hub's home screen.
 *
 * <p>Resolved server-side and pushed to the client through the hub's bindings: LuckPerms has no
 * client presence at all — the API is {@code compileOnly} and the jar ships only with the dedicated
 * server — so there is nothing for a client to read.
 *
 * <p>Same soft-dependency shape as {@link LuckPermsCompat}: the API is only touched from the nested
 * {@link Lp} holder, which is never loaded unless LuckPerms is present.
 */
public final class RankResolver {

    /** What a player with no groups, or no LuckPerms at all, is shown as. */
    public static final String DEFAULT_RANK = "player";

    private RankResolver() {}

    /**
     * The player's highest-weighted inherited group, falling back to their primary group and then
     * to {@link #DEFAULT_RANK}. Weight is the right measure because a player is usually in several
     * groups at once and only the most senior one is worth showing.
     */
    public static String rankId(ServerPlayer player) {
        if (LuckPermsCompat.LOADED) {
            try {
                String rank = Lp.highestGroup(player);
                if (rank != null && !rank.isEmpty()) return rank;
            } catch (Throwable ignored) {
                // LuckPerms not ready, or the user is not loaded yet — show the default.
            }
        }
        return DEFAULT_RANK;
    }

    /**
     * The rank as text. A group gets a pretty name from {@code rank.roll_mod.<id>} when that key
     * exists, so ranks can be retitled in lang; otherwise the raw group id is shown, which means a
     * new group on the server needs no code change to appear correctly.
     */
    public static Component displayName(String rankId) {
        String key = "rank." + RollMod.MODID + "." + rankId;
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(rankId);
    }

    /** The lowest staff group. Everything weighted at or above it counts as staff. */
    public static final String HELPER_RANK = "helper";

    /**
     * Whether the player is {@link #HELPER_RANK} or higher: they inherit the helper group, or hold a
     * group weighted at least as heavily as it. Weight is what makes "higher" work without every
     * senior group having to inherit helper. False without LuckPerms, or when no helper group exists.
     */
    public static boolean isHelperOrHigher(ServerPlayer player) {
        if (!LuckPermsCompat.LOADED) return false;
        try {
            return Lp.atLeast(player, HELPER_RANK);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Isolated holder for the LuckPerms API; only referenced when {@link LuckPermsCompat#LOADED}. */
    private static final class Lp {

        static String highestGroup(ServerPlayer player) {
            net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
            net.luckperms.api.model.user.User user = lp.getUserManager().getUser(player.getUUID());
            if (user == null) return null;

            net.luckperms.api.query.QueryOptions options =
                    lp.getContextManager().getQueryOptions(user)
                            .orElse(net.luckperms.api.query.QueryOptions.defaultContextualOptions());

            String best = null;
            int bestWeight = Integer.MIN_VALUE;
            for (net.luckperms.api.model.group.Group group : user.getInheritedGroups(options)) {
                // Unweighted groups sort below every weighted one rather than above them.
                int weight = group.getWeight().orElse(Integer.MIN_VALUE + 1);
                if (weight > bestWeight) {
                    bestWeight = weight;
                    best = group.getName();
                }
            }
            return best != null ? best : user.getPrimaryGroup();
        }

        static boolean atLeast(ServerPlayer player, String groupName) {
            net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
            net.luckperms.api.model.group.Group floor = lp.getGroupManager().getGroup(groupName);
            if (floor == null) return false;
            net.luckperms.api.model.user.User user = lp.getUserManager().getUser(player.getUUID());
            if (user == null) return false;

            net.luckperms.api.query.QueryOptions options =
                    lp.getContextManager().getQueryOptions(user)
                            .orElse(net.luckperms.api.query.QueryOptions.defaultContextualOptions());

            java.util.OptionalInt floorWeight = floor.getWeight();
            for (net.luckperms.api.model.group.Group group : user.getInheritedGroups(options)) {
                if (group.getName().equals(floor.getName())) return true;
                if (floorWeight.isPresent() && group.getWeight().isPresent()
                        && group.getWeight().getAsInt() >= floorWeight.getAsInt()) return true;
            }
            return false;
        }
    }
}
