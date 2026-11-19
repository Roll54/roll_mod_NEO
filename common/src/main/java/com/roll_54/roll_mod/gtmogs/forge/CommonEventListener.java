package com.roll_54.roll_mod.gtmogs.forge;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.ServerStallWatchdog;
import com.roll_54.roll_mod.gtmogs.common.data.loader.PostRegistryListener;

@EventBusSubscriber(modid = GTMOGS.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommonEventListener {

    @SubscribeEvent
    public static void registerReloadListeners(AddReloadListenerEvent event) {
        GTRegistries.updateFrozenRegistry(event.getRegistryAccess());
        event.addListener(PostRegistryListener.INSTANCE);
    }

    @SubscribeEvent
    public static void levelTickStart(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ServerStallWatchdog.onLevelTickStart(serverLevel);
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ServerStallWatchdog.onLevelTickEnd(serverLevel);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        ServerStallWatchdog.onServerTick(Thread.currentThread());
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) {
        ServerStallWatchdog.start();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ServerStallWatchdog.stop();
    }
}
