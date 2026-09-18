package com.roll_54.roll_mod.cosmetics;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Keeps every client's picture of who is wearing what up to date across joins, leaves and respawns,
 * and drops the slot classification cache whenever the item tags behind it change.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ItemSkinEvents {
    private ItemSkinEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemSkinService.onJoin(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemSkinService.onLeave(player);
        }
    }

    /**
     * Respawning and dimension changes hand the player a new entity instance. The cosmetics
     * themselves are keyed by UUID and are already loaded, and other players' renderers pick the new
     * entity up immediately, so the safe move is simply to re-announce.
     *
     * <p>{@code resync}, not {@code onJoin}: since the data moved into a database, {@code onJoin}
     * means "read this player from storage", and doing that on every death would be pure waste.
     */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemSkinService.resync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemSkinService.broadcastActive(player);
        }
    }

    /**
     * {@link SkinCategory#of} memoises per item, and the first thing it consults is the skin slot
     * tags. Without this the memo would outlive the tags it was derived from, and a {@code /reload}
     * that blacklists or retags an item would silently appear to do nothing until the game restarted.
     *
     * <p>Fires on both sides — on the server after a datapack reload, on the client after it receives
     * the synced tags — which is exactly the coverage needed, since both resolve slots independently.
     */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        SkinCategory.invalidate();
    }
}
