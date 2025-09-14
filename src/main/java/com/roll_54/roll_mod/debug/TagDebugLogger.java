package com.roll_54.roll_mod.debug;

import com.roll_54.roll_mod.Roll_mod;
import com.roll_54.roll_mod.netherstorm.StormHandler;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Roll_mod.MODID)
public class TagDebugLogger {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var server   = event.getServer();
        var itemsRef = server.registryAccess().lookupOrThrow(Registries.ITEM);
        var tagKey   = StormHandler.STORM_PROTECTIVE_TAG;

        itemsRef.get(tagKey).ifPresentOrElse(tagSet -> {
            Roll_mod.LOGGER.info("[storm_protective] Loaded {} entries:", tagSet.size());
            tagSet.forEach(holder -> holder.unwrapKey().ifPresentOrElse(
                    key -> Roll_mod.LOGGER.info(" - {}", key.location()),
                    ()  -> Roll_mod.LOGGER.info(" - <unbound> {}", holder.value()) // на всяк випадок
            ));
        }, () -> {
            Roll_mod.LOGGER.warn("[storm_protective] Tag NOT found or empty!");
        });
    }
}
// ГІГА ДЕБАГ ХУЙНЯ, ШИЗО-КОД!!!!!!!!!!!!!!!!!!!!!!!!!