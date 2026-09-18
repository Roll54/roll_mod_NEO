package com.roll_54.roll_mod_client.client.skin.item;

import com.roll_54.roll_mod.cosmetics.client.ClientItemSkinCache;
import com.roll_54.roll_mod_client.RollModClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Drops the skin cache on disconnect, so a new session never inherits the last one's players. */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class ItemSkinClientEvents {
    private ItemSkinClientEvents() {}

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientItemSkinCache.clear();
    }
}
