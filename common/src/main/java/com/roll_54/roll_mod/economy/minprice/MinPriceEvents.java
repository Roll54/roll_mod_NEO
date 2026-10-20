package com.roll_54.roll_mod.economy.minprice;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * When the minimal price table is (re)built: server start, {@code /reload}, a config edit; and the
 * client's copy for a player who joins.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class MinPriceEvents {

  /** Set once the server has started; the first tag load happens before that and is not ours. */
  private static volatile boolean started;

  private MinPriceEvents() {}

  @SubscribeEvent
  public static void onServerStarted(ServerStartedEvent event) {
    started = true;
    MinPriceService.rebuild(event.getServer());
  }

  @SubscribeEvent
  public static void onTagsUpdated(TagsUpdatedEvent event) {
    if (!started || event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    // Queued, so the build reads the recipe manager once the reload has fully finished.
    if (server != null) server.execute(() -> MinPriceService.rebuild(server));
  }

  @SubscribeEvent
  public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) MinPriceService.sendTo(player);
  }

  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    started = false;
    MinPriceService.clear();
  }

  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {
    MinPriceCommand.register(event.getDispatcher(), event.getBuildContext());
  }
}
