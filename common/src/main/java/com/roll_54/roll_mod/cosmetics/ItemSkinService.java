package com.roll_54.roll_mod.cosmetics;

import com.roll_54.roll_mod.cosmetics.storage.ItemSkinStore;
import com.roll_54.roll_mod.cosmetics.storage.ItemSkinStore.Result;
import com.roll_54.roll_mod.network.packet.skin.SyncActiveSkinsPacket;
import com.roll_54.roll_mod.network.packet.skin.SyncUnlockedSkinsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

/**
 * Server-side entry point for everything that reads or changes a player's cosmetic skins.
 *
 * <p>Every mutation re-validates from scratch and then syncs, in that order. The client's request
 * is only ever a suggestion — it is sent optimistically and a spoofed one achieves nothing, which is
 * the same contract {@code HomeService} works under.
 *
 * <p>The state itself lives in {@link ItemSkinStore}, backed by a database outside the world save,
 * because cosmetics are permanent entitlements that must survive a world reset. This class stays
 * synchronous: the store answers from memory and queues the SQL, so callers keep getting an
 * immediate answer. The one new meaning of {@code false} is "not loaded yet, or storage is down" —
 * {@link #isReady} tells the two apart when a caller needs to explain itself.
 */
public final class ItemSkinService {
    private ItemSkinService() {}

    public static PlayerItemSkins get(ServerPlayer player) {
        return ItemSkinStore.get(player.getUUID());
    }

    /** Whether this player's cosmetics are loaded and changeable right now. */
    public static boolean isReady(ServerPlayer player) {
        return ItemSkinStore.isReady(player.getUUID());
    }

    /** Grants a skin. Returns false when the id is unknown or the player already owned it. */
    public static boolean unlock(ServerPlayer player, ResourceLocation skinId) {
        if (ItemSkinRegistry.get(skinId) == null) {
            return false;
        }
        // Already owning it is a failure here, matching the pre-database behaviour.
        if (ItemSkinStore.unlock(player.getUUID(), skinId) != Result.CHANGED) {
            return false;
        }
        syncUnlocked(player);
        return true;
    }

    /** Revokes a skin, and stops it rendering if it was active. */
    public static boolean revoke(ServerPlayer player, ResourceLocation skinId) {
        if (ItemSkinStore.revoke(player.getUUID(), skinId) != Result.CHANGED) {
            return false;
        }
        syncUnlocked(player);
        // The active half may have changed too, and other clients need to stop drawing it.
        broadcastActive(player);
        return true;
    }

    /**
     * Makes {@code skinId} render on every item in its own slot for this player.
     *
     * <p>The slot comes from the skin itself rather than from the caller, so a client cannot ask for
     * a sword skin in the helmet slot. Rejects an unknown skin and one the player has not unlocked.
     */
    public static boolean setActive(ServerPlayer player, ResourceLocation skinId) {
        ItemSkinDefinition skin = ItemSkinRegistry.get(skinId);
        if (skin == null) {
            return false;
        }
        if (!get(player).hasUnlocked(skinId)) {
            return false;
        }
        Result result = ItemSkinStore.setActive(player.getUUID(), skin.category(), skinId);
        if (result == Result.UNAVAILABLE) {
            return false;
        }
        // Re-selecting the skin that is already in that slot is a success, not a failure — the
        // caller asked for a state and that state holds. Only re-broadcast if something moved.
        if (result == Result.CHANGED) {
            broadcastActive(player);
        }
        return true;
    }

    public static boolean clearActive(ServerPlayer player, SkinCategory category) {
        // Clearing an empty slot stays a failure: there was nothing to clear.
        if (ItemSkinStore.clearActive(player.getUUID(), category) != Result.CHANGED) {
            return false;
        }
        broadcastActive(player);
        return true;
    }

    /** The active half goes to everyone, since anyone may be looking at this player. */
    public static void broadcastActive(ServerPlayer player) {
        PacketDistributor.sendToAllPlayers(
                new SyncActiveSkinsPacket(player.getUUID(), get(player).active()));
    }

    /** The unlocked half never leaves its owner. */
    public static void syncUnlocked(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncUnlockedSkinsPacket(get(player).unlocked()));
    }

    /**
     * Loads this player's cosmetics from storage, then announces them. Login only.
     *
     * <p>The announcement is deferred until the load returns, so what goes out immediately is the
     * empty state. That is correct rather than a compromise: {@code ClientItemSkinCache} reads an
     * empty map as "forget this player", which is exactly the stale-entry purge a rejoining UUID
     * needs. The real state follows a tick or two later, and the client swaps the whole map at once.
     */
    public static void onJoin(ServerPlayer player) {
        broadcastActive(player);
        ItemSkinStore.onJoin(player);
    }

    /**
     * Re-announces this player in both directions, without touching storage.
     *
     * <p>Used by the load completion, and by respawn — which hands out a new entity instance but no
     * new cosmetics, so re-reading the database there would be pure waste.
     */
    public static void resync(ServerPlayer player) {
        syncUnlocked(player);
        broadcastActive(player);
        // A joining player needs everyone else's skins too; neither client can derive the other's.
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player) {
                PacketDistributor.sendToPlayer(player,
                        new SyncActiveSkinsPacket(other.getUUID(), get(other).active()));
            }
        }
    }

    /** An empty map is the cache's "forget this player" signal. */
    public static void onLeave(ServerPlayer player) {
        ItemSkinStore.onLeave(player.getUUID());
        PacketDistributor.sendToAllPlayers(new SyncActiveSkinsPacket(player.getUUID(), Map.of()));
    }
}
