package com.roll_54.roll_mod.minestar.hub;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.home.HomeViewers;
import com.roll_54.roll_mod.minestar.kits.KitViewers;
import com.roll_54.roll_mod.minestar.op.OperatorViewers;
import com.roll_54.roll_mod.minestar.tpa.TpaService;
import com.roll_54.roll_mod.minestar.tpa.TpaViewers;
import com.roll_54.roll_mod.minestar.rtp.RtpService;
import com.roll_54.roll_mod.minestar.teleport.TeleportService;
import com.roll_54.roll_mod.minestar.hub.warp.WarpService;
import com.roll_54.roll_mod.minestar.hub.warp.WarpViewers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerPlayer;

/** Server-side upkeep for the hub: teleport countdowns, and forgetting players who have left. */
@EventBusSubscriber(modid = RollMod.MODID)
public final class HubEvents {

    private HubEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TeleportService.tick(event.getServer());
        RtpService.tick(event.getServer());
        TpaService.tick(event.getServer());
    }

    /** The tpa tab lists who is online, so an arrival changes what every open hub should show. */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TpaViewers.resync(player.server);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        WarpService.cancel(event.getEntity().getUUID());
        RtpService.cancel(event.getEntity().getUUID());
        WarpViewers.remove(event.getEntity().getUUID());
        HomeViewers.remove(event.getEntity().getUUID());
        KitViewers.remove(event.getEntity().getUUID());
        TpaViewers.remove(event.getEntity().getUUID());
        OperatorViewers.remove(event.getEntity().getUUID());
        TpaService.forget(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            TpaViewers.resync(player.server);
        }
    }
}
