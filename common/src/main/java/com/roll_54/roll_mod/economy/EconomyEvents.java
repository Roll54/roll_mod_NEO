package com.roll_54.roll_mod.economy;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.command.CurrencyCommand;
import com.roll_54.roll_mod.economy.config.CurrencyConfig;
import com.roll_54.roll_mod.economy.database.DatabaseManager;
import com.roll_54.roll_mod.economy.database.SchemaMigrator;
import com.roll_54.roll_mod.economy.plot.PlotService;
import com.roll_54.roll_mod.economy.plot.PlotViewers;
import com.roll_54.roll_mod.economy.plot.command.PlotCommand;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionManager;
import com.roll_54.roll_mod.economy.vendingblock.command.AuctionCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The economy's game-bus handlers: the currency database's lifecycle, auction expiry, and the
 * commands.
 *
 * <p>These were instance methods on the old {@code roll_mod_currency} entrypoint, which registered
 * itself on {@code NeoForge.EVENT_BUS}. With one mod there is one entrypoint, so they live here
 * instead — the same shape as {@code DailyTaskEvents}.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class EconomyEvents {

  /** Expire auction listings roughly every five seconds. */
  private static final int AUCTION_EXPIRY_INTERVAL_TICKS = 100;

  private static int auctionTickCounter = 0;

  private EconomyEvents() {}

  @SubscribeEvent
  public static void onServerStarting(ServerStartingEvent event) {
    // The SQL driver is shaded into the dedicated-server jar only (see server/build.gradle), so a
    // production client running a singleplayer world has none. Degrade to "no economy" rather than
    // taking the whole world down with it.
    try {
      DatabaseManager.getInstance().init(CurrencyConfig.MAIN.sql);
      // Block until migrations finish: the schema has to exist before any balance is read.
      SchemaMigrator.migrate().join();
      RollMod.LOGGER.info("[Economy] Database ready.");
    } catch (Throwable t) {
      RollMod.LOGGER.error(
          "[Economy] Database unavailable; currency is disabled for this session.", t);
    }
  }

  /**
   * Once OPAC's claims are loaded, close any plot left as wild land by the old sell-to-server, which
   * unclaimed instead of handing the plot back to the server claim.
   */
  @SubscribeEvent
  public static void onServerStarted(ServerStartedEvent event) {
    try {
      PlotService.protectVacantPlots(event.getServer());
    } catch (Throwable t) {
      RollMod.LOGGER.error("[Economy] Could not repair plot protection.", t);
    }
  }

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    if (++auctionTickCounter >= AUCTION_EXPIRY_INTERVAL_TICKS) {
      auctionTickCounter = 0;
      AuctionManager.tickExpiry(event.getServer());
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    AuctionManager.removeViewer(event.getEntity().getUUID());
    PlotViewers.remove(event.getEntity().getUUID());
  }

  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    DatabaseManager.getInstance().shutdown();
  }

  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {
    CurrencyCommand.register(event.getDispatcher());
    AuctionCommand.register(event);
    PlotCommand.register(event);
  }
}
