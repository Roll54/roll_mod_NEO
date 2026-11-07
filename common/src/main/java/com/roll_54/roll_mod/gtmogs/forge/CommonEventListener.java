package com.roll_54.roll_mod.gtmogs.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.ServerStallWatchdog;
import com.roll_54.roll_mod.gtmogs.common.capability.WorldIDSaveData;
import com.roll_54.roll_mod.gtmogs.common.data.loader.PostRegistryListener;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.data.command.GTCommands;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;
import com.roll_54.roll_mod.gtmogs.integration.map.ClientCacheManager;
import com.roll_54.roll_mod.gtmogs.integration.map.WaypointManager;
import com.roll_54.roll_mod.gtmogs.integration.map.cache.server.ServerCache;
import com.roll_54.roll_mod.gtmogs.utils.TaskHandler;

@EventBusSubscriber(modid = GTMOGS.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommonEventListener {

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        GTCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();

        if (!level.isClientSide) {
            BlockPos pos = event.getHitVec().getBlockPos();
            Block blockClicked = level.getBlockState(pos).getBlock();
            if (GTOreVeins.getVeinOres().contains(blockClicked)) {
                ServerCache.instance.prospectByOreMaterial(
                        level.dimension(),
                        pos,
                        (ServerPlayer) event.getEntity(), ConfigHolder.INSTANCE.compat.minimap.oreBlockProspectRange);
            }
        }
    }

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
            TaskHandler.onTickUpdate(serverLevel);
            ServerStallWatchdog.onLevelTickEnd(serverLevel);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        ServerStallWatchdog.onServerTick(Thread.currentThread());
    }

    @SubscribeEvent
    public static void worldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            WaypointManager.updateDimension(event.getLevel());
        } else if (event.getLevel() instanceof ServerLevel serverLevel) {
            ServerCache.instance.maybeInitWorld(serverLevel);
        }
    }

    @SubscribeEvent
    public static void worldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TaskHandler.onWorldUnLoad(serverLevel);
            ServerCache.instance.invalidateWorld(serverLevel);
        } else if (event.getLevel().isClientSide()) {
            ClientCacheManager.saveCaches(event.getLevel().registryAccess());
        }
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) {
        ServerLevel mainLevel = event.getServer().overworld();
        WorldIDSaveData.init(mainLevel);
        ServerStallWatchdog.start();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ServerStallWatchdog.stop();
        ServerCache.instance.clear();
    }
}
