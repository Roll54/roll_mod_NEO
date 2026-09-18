package com.roll_54.roll_mod.economy.currency;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.config.CurrencyConfig;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.network.payload.PlaytimeRewardPayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Awards currency automatically based on how long players are online.
 *
 * <ul>
 *   <li><b>Persistent</b> reward: every {@code persistentIntervalMinutes} of <i>accumulated</i>
 *       playtime grants {@code persistentReward}. The accumulated time is stored in the player's
 *       persistent NBT data (vanilla save system), so it survives logout, death, and server
 *       restarts &mdash; no database table involved.
 *   <li><b>Session</b> reward: every {@code sessionIntervalMinutes} of <i>continuous online</i>
 *       time grants {@code sessionReward}. This counter lives only in memory and resets to 0 on
 *       every login, so progress is lost if the player leaves before reaching the threshold.
 * </ul>
 *
 * Time is accrued by counting server ticks while the player is online (1 second per 20 ticks), so
 * it only advances while the player is actually connected and the server is running.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class PlaytimeManager {

  private PlaytimeManager() {}

  private static final int TICKS_PER_SECOND = 20;

  /** Key under which accumulated persistent playtime (in seconds) is stored in player NBT. */
  // Compatibility name, not an oversight: this key is already in every live player's NBT.
  // Renaming it to the roll_mod namespace would reset everyone's accumulated playtime to zero.
  private static final String NBT_PERSISTENT_SECONDS = "roll_mod_currency:persistent_seconds";

  /** Non-persistent per-player session seconds (single-element array used as a mutable counter). */
  private static final Map<UUID, long[]> SESSION_SECONDS = new ConcurrentHashMap<>();

  /** Counts server ticks so the reward logic runs once per real second. */
  private static int tickCounter = 0;

  @SubscribeEvent
  public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    // Session timer always starts fresh on login (non-persistent by design).
    SESSION_SECONDS.put(player.getUUID(), new long[] {0L});
  }

  @SubscribeEvent
  public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    SESSION_SECONDS.remove(player.getUUID());
  }

  @SubscribeEvent
  public static void onPlayerClone(PlayerEvent.Clone event) {
    // getPersistentData() persists across logout/restart on its own, but the player entity is
    // recreated on death and dimension change, so carry the accumulated playtime over here.
    CompoundTag oldData = event.getOriginal().getPersistentData();
    if (oldData.contains(NBT_PERSISTENT_SECONDS)) {
      event
          .getEntity()
          .getPersistentData()
          .putLong(NBT_PERSISTENT_SECONDS, oldData.getLong(NBT_PERSISTENT_SECONDS));
    }
  }

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    CurrencyConfig.PlaytimeSettings cfg = settings();
    if (cfg == null || !cfg.enabled) return;

    if (++tickCounter < TICKS_PER_SECOND) return;
    tickCounter = 0;

    CurrencyType type = currencyType(cfg);
    long persistentIntervalSeconds = (long) cfg.persistentIntervalMinutes * 60;
    long sessionIntervalSeconds = (long) cfg.sessionIntervalMinutes * 60;

    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
      // --- Persistent reward (stored in vanilla player NBT) ---
      if (persistentIntervalSeconds > 0 && cfg.persistentReward > 0) {
        CompoundTag data = player.getPersistentData();
        long seconds = data.getLong(NBT_PERSISTENT_SECONDS) + 1;
        data.putLong(NBT_PERSISTENT_SECONDS, seconds);
        if (seconds % persistentIntervalSeconds == 0) {
          grant(player, type, cfg.persistentReward, false);
        }
      }

      // --- Session reward (in-memory, non-persistent) ---
      long[] session = SESSION_SECONDS.get(player.getUUID());
      if (session != null && sessionIntervalSeconds > 0 && cfg.sessionReward > 0) {
        session[0]++;
        if (session[0] % sessionIntervalSeconds == 0) {
          grant(player, type, cfg.sessionReward, true);
        }
      }
    }
  }

  /**
   * Deposits a reward and, on success, notifies the player's client so it can print a chat message
   * (the client decides whether to actually show it, via its config toggle).
   */
  private static void grant(ServerPlayer player, CurrencyType type, long amount, boolean session) {
    CurrencyService.deposit(player, type, amount, player.server)
        .thenAcceptAsync(
            success -> {
              if (success) {
                PacketDistributor.sendToPlayer(
                    player, new PlaytimeRewardPayload(amount, type, session));
              }
            },
            player.server);
  }

  private static CurrencyConfig.PlaytimeSettings settings() {
    return CurrencyConfig.MAIN != null ? CurrencyConfig.MAIN.playtime : null;
  }

  private static CurrencyType currencyType(CurrencyConfig.PlaytimeSettings cfg) {
    if (cfg.currencyType != null) {
      for (CurrencyType type : CurrencyType.values()) {
        if (type.id().equalsIgnoreCase(cfg.currencyType)) {
          return type;
        }
      }
    }
    return CurrencyType.MAIN;
  }
}
