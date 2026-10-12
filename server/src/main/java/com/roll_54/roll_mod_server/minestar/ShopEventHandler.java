package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod_server.RollModServer;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import static ua.com.minestar.Minestar.minestar;

@EventBusSubscriber(modid = RollModServer.MODID, value = Dist.DEDICATED_SERVER)
public class ShopEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Purchases arrive as letters now; a moment's delay, as the old notice had, so the
            // player is fully in before the chat line.
            minestar.getScheduler().schedule(() -> player.server.execute(() -> ShopLetters.deliver(player, false)),
                    3, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

}