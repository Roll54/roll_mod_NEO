package com.roll_54.roll_mod.minestar.hub;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.home.HomeViewers;
import com.roll_54.roll_mod.minestar.hub.warp.WarpService;
import com.roll_54.roll_mod.minestar.hub.warp.WarpViewers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerPlayer;

/** Server-side upkeep for the hub: warp warm-ups, and forgetting players who have left. */
@EventBusSubscriber(modid = RollMod.MODID)
public final class HubEvents {

    private HubEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WarpService.tick(event.getServer());
    }

    /**
     * Taking a hit cancels a pending warp. Without this the warm-up would be no protection at all —
     * a player could open the hub mid-fight and leave regardless.
     */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WarpService.cancel(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        WarpService.cancel(event.getEntity().getUUID());
        WarpViewers.remove(event.getEntity().getUUID());
        HomeViewers.remove(event.getEntity().getUUID());
    }
}
