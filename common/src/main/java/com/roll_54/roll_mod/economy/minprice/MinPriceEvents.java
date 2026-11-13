package com.roll_54.roll_mod.economy.minprice;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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

  /**
   * {@code /rollmod admin minprice auto all}: the shortest recipe for every item and fluid, replacing the
   * chosen ones; {@code ... auto missing}: only for those with no chosen recipe and no fixed price yet. See
   * {@link MinPriceAuto}. Prices are rebuilt right after.
   */
  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("rollmod")
        .then(Commands.literal("admin")
            .requires(source -> source.hasPermission(SaveMinPriceRecipesPacket.PERMISSION_LEVEL))
            .then(Commands.literal("minprice")
                .then(Commands.literal("auto")
                    .then(Commands.literal("all").executes(ctx -> auto(ctx.getSource(), true)))
                    .then(Commands.literal("missing").executes(ctx -> auto(ctx.getSource(), false)))))));
  }

  private static int auto(CommandSourceStack source, boolean all) {
    MinPriceAuto.Result result = MinPriceAuto.run(source.getServer(), all);
    source.sendSuccess(() -> Component.translatable(
        all ? "msg.roll_mod.minprice.auto_all" : "msg.roll_mod.minprice.auto_missing",
        result.picked(), result.blacklisted()), true);
    return result.picked();
  }

  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    started = false;
    MinPriceService.clear();
  }
}
