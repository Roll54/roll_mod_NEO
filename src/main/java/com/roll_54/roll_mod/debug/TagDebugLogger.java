package com.roll_54.roll_mod.debug;

import com.roll_54.roll_mod.сonfig.GeneralConfig;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.Netherstorm.StormHandler;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = RollMod.MODID)

public class TagDebugLogger {
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!GeneralConfig.isDebug) return; // читаємо кеш!

        var server = event.getServer();
        var itemsRef = server.registryAccess().lookupOrThrow(Registries.ITEM);
        var tagKey = StormHandler.STORM_PROTECTIVE_TAG;

        itemsRef.get(tagKey).ifPresentOrElse(tagSet -> {
            RollMod.LOGGER.info("[storm_protective] Loaded {} entries:", tagSet.size());
            tagSet.forEach(holder -> holder.unwrapKey().ifPresentOrElse(
                    key -> RollMod.LOGGER.info(" - {}", key.location()),
                    () -> RollMod.LOGGER.info(" - <unbound> {}", holder.value())
            ));
        }, () -> {
            RollMod.LOGGER.warn("[storm_protective] Tag NOT found or empty!");
        });
    }
}