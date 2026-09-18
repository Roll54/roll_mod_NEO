package com.roll_54.roll_mod.cosmetics.storage;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.cosmetics.config.CosmeticsConfig;
import java.time.Duration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Opens the cosmetics database when a world starts and closes it cleanly when one stops. */
@EventBusSubscriber(modid = RollMod.MODID)
public final class CosmeticsStorageEvents {

  /** Long enough for a queued write to finish, short enough not to hang a shutdown. */
  private static final Duration FLUSH_TIMEOUT = Duration.ofSeconds(10);

  private CosmeticsStorageEvents() {}

  /**
   * Unlike the economy, this defaults to an embedded file, so it succeeds on a fresh install with no
   * configuration at all — including in singleplayer, which is where skins get tested.
   */
  @SubscribeEvent
  public static void onServerStarting(ServerStartingEvent event) {
    try {
      CosmeticsDatabase.getInstance().init(CosmeticsConfig.MAIN.storage);
      if (CosmeticsDatabase.getInstance().isAvailable()) {
        // Block until the schema exists: the first player may log in immediately after this.
        CosmeticsSchemaMigrator.migrate().join();
        RollMod.LOGGER.info("[Cosmetics] Skin storage ready.");
      }
    } catch (Throwable t) {
      RollMod.LOGGER.error("[Cosmetics] Skin storage unavailable; cosmetics are disabled for this "
              + "session. Nothing will be written, so nothing is lost.", t);
      // Leave nothing half-open, and let the next world try again from scratch.
      CosmeticsDatabase.getInstance().shutdown();
    }
  }

  /** Order matters: drain the queued writes before the pool that would run them is closed. */
  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    ItemSkinStore.flush(FLUSH_TIMEOUT);
    CosmeticsDatabase.getInstance().shutdown();
  }
}
