package com.roll_54.roll_mod.minestar.welcome;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Opens the starting screen for players who have not accepted it, and kicks on a close. */
@EventBusSubscriber(modid = RollMod.MODID)
public final class WelcomeEvents {

    /**
     * How long after login the screen opens. Opened during login itself, it races the client's
     * terrain-loading screen, which can replace it.
     */
    private static final int OPEN_DELAY_TICKS = 40;

    /** Players waiting for their screen, with the server tick it is due on. */
    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    private WelcomeEvents() {}

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !WelcomeService.accepted(player)) {
            PENDING.put(player.getUUID(), player.server.getTickCount() + OPEN_DELAY_TICKS);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        int now = event.getServer().getTickCount();
        Iterator<Map.Entry<UUID, Integer>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            if (entry.getValue() > now) continue;
            it.remove();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && !player.hasDisconnected() && !WelcomeService.accepted(player)) {
                WelcomeService.open(player);
            }
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WelcomeService.onClosed(player, event.getContainer());
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
        WelcomeService.forget(event.getEntity().getUUID());
    }
}
