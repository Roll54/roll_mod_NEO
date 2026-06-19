package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod_server.RollModServer;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = RollModServer.MODID, value = Dist.DEDICATED_SERVER)
public class ShopEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShopCheckProductsAndSendMessage.checkAndNotifyOnLogin(player);
        }
    }

}