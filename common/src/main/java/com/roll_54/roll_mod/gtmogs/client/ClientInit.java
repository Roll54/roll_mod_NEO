package com.roll_54.roll_mod.gtmogs.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.GTValues;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.integration.map.ClientCacheManager;
import com.roll_54.roll_mod.gtmogs.integration.map.cache.client.GTClientCache;
import com.roll_54.roll_mod.gtmogs.integration.map.ftbchunks.FTBChunksPlugin;
import com.roll_54.roll_mod.gtmogs.integration.map.layer.Layers;
import com.roll_54.roll_mod.gtmogs.integration.map.layer.builtin.OreRenderLayer;
import com.roll_54.roll_mod.gtmogs.utils.input.KeyBind;

public class ClientInit {

    public static void init(IEventBus modBus) {
        modBus.register(ClientInit.class);
        if (!GTMOGS.isDataGen()) {
            ClientCacheManager.registerClientCache(GTClientCache.instance, "gtmogs");
            Layers.registerLayer(OreRenderLayer::new, "ore_veins");
        }
    }

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        KeyBind.onRegisterKeyBinds(event);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ConfigHolder.INSTANCE.compat.minimap.toggle.ftbChunksIntegration &&
                GTMOGS.isModLoaded(GTValues.MODID_FTB_CHUNKS)) {
            FTBChunksPlugin.addEventListeners();
        }
    }
}
