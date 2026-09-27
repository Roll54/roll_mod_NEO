package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import net.minecraft.server.level.ServerPlayer;

/**
 * Who may moderate, in one place.
 *
 * <p>Every one of these folds operators back in, the way {@code WarpService.isModerator} does:
 * {@code LuckPermsCompat}'s own accessors answer {@code false} without LuckPerms, which on a
 * single-player world or a server that never installed it would refuse everybody including the
 * person who owns the machine. The node is the interesting answer; being an operator is the
 * backstop.
 */
public final class ModerationPermissions {

    private ModerationPermissions() {}

    /**
     * May open the moderation tab, mute, warn, and act on warps.
     *
     * <p>{@code rollmod.warps.moderate} counts too, and not as an afterthought: it is documented in
     * {@code docs/luckperms-keys.txt} as "what moderation and higher means on this server", so it
     * is the node this server's staff group already holds. Accepting it means the moderation tab
     * appears for the existing staff on the day this ships instead of silently demoting them.
     */
    public static boolean canModerate(ServerPlayer player) {
        return player.hasPermissions(2)
                || LuckPermsCompat.canModerate(player)
                || LuckPermsCompat.canModerateWarps(player);
    }

    /** May ban, unban, and open or close the server. Deliberately stricter than {@link #canModerate}. */
    public static boolean canBan(ServerPlayer player) {
        return player.hasPermissions(2) || LuckPermsCompat.canBan(player);
    }

    /** May write letters. Reading them needs nothing at all. */
    public static boolean canWriteLetters(ServerPlayer player) {
        return player.hasPermissions(2) || LuckPermsCompat.canWriteLetters(player);
    }

    /**
     * Whether the whitelist lets this player through. The same answer as {@link #canModerate}, kept
     * under its own name so the login gate reads as what it is and so the two can part company
     * later without hunting down call sites.
     */
    public static boolean bypassesWhitelist(ServerPlayer player) {
        return canModerate(player);
    }
}
