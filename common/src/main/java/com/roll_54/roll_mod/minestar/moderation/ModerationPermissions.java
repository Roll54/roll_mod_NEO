package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;

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

    /** May read the death log and restore deaths from it — which hands out items, so kept apart. */
    public static boolean canUseDeathlog(ServerPlayer player) {
        return player.hasPermissions(2) || LuckPermsCompat.canUseDeathlog(player);
    }

    /** May open another player's inventory, curios and implants, read-only unless {@link #canInvEdit}. */
    public static boolean canInvSee(ServerPlayer player) {
        return canInvEdit(player) || LuckPermsCompat.canInvSee(player);
    }

    /** May change another player's items. Implies {@link #canInvSee}. */
    public static boolean canInvEdit(ServerPlayer player) {
        return player.hasPermissions(2) || LuckPermsCompat.canInvEdit(player);
    }

    /**
     * Whether the whitelist lets this player through. The same answer as {@link #canModerate}, kept
     * under its own name so the login gate reads as what it is and so the two can part company
     * later without hunting down call sites.
     */
    public static boolean bypassesWhitelist(ServerPlayer player) {
        return canModerate(player);
    }

    /**
     * {@link #bypassesWhitelist(ServerPlayer)} for the login handshake, where there is no player yet
     * — only a profile. Operators by the ops list, staff by LuckPerms' stored data.
     */
    public static boolean bypassesWhitelist(MinecraftServer server, GameProfile profile) {
        ServerOpListEntry op = server.getPlayerList().getOps().get(profile);
        if (op != null && op.getLevel() >= 2) return true;
        return LuckPermsCompat.canModerateOffline(profile.getId());
    }
}
