package com.roll_54.roll_mod.gtmogs.client.forge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.GTValues;
import com.roll_54.roll_mod.gtmogs.data.command.GTClientCommands;
import com.roll_54.roll_mod.gtmogs.integration.map.ClientCacheManager;

@EventBusSubscriber(modid = GTMOGS.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ForgeClientEventListener {

    @SubscribeEvent
    public static void onClientTickEvent(ClientTickEvent.Post event) {
        GTValues.CLIENT_TIME++;
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCacheManager.allowReinit();
    }

    @SubscribeEvent
    public static void registerClientCommand(RegisterClientCommandsEvent event) {
        GTClientCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ClientCacheManager.clearCaches();
    }
}
