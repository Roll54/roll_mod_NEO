package com.roll_54.roll_mod.minestar.hub;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.PlayerPositions;
import com.roll_54.roll_mod.minestar.hub.home.HomeViewers;
import com.roll_54.roll_mod.minestar.kits.KitViewers;
import com.roll_54.roll_mod.minestar.letters.LetterService;
import com.roll_54.roll_mod.minestar.letters.LetterViewers;
import com.roll_54.roll_mod.minestar.moderation.ModerationViewers;
import com.roll_54.roll_mod.minestar.moderation.TpsMonitor;
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
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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
        // Lives here rather than in :server because the hub's TPS readout has to read it, and
        // :server is dedicated-server-only and invisible to the UI.
        TpsMonitor.tick(event.getServer());
        ModerationViewers.tick(event.getServer());
        LetterService.tick(event.getServer());
        PlayerPositions.tick(event.getServer());
    }

    /** Back positions batch to disk on an interval, so a stop must write out what is pending. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PlayerPositions.flush();
    }

    /** The tpa tab lists who is online, so an arrival changes what every open hub should show. */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TpaViewers.resync(player.server);
        }
        // The moderation list is the online roster, so an arrival is a change to it.
        ModerationViewers.markDirty();
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
        ModerationViewers.remove(event.getEntity().getUUID());
        LetterViewers.remove(event.getEntity().getUUID());
        ModerationViewers.markDirty();
        TpaService.forget(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            TpaViewers.resync(player.server);
        }
    }
}
