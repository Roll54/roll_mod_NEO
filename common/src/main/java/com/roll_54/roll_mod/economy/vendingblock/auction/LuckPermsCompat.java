package com.roll_54.roll_mod.economy.vendingblock.auction;

import com.roll_54.roll_mod.economy.vendingblock.Config;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Soft LuckPerms integration. The actual LuckPerms API is only touched from the nested {@link Lp}
 * class, which is never loaded unless {@link #LOADED} is true — so the mod runs fine without
 * LuckPerms installed (the API is a {@code compileOnly} dependency).
 *
 * <ul>
 *   <li>Permission {@code rollmod.ah.prime} &rarr; longer listing duration cap (config
 *       "ahPrimeDays").
 *   <li>Meta {@code rollmod.ah.positions} (integer) &rarr; per-player max active listings.
 *   <li>Meta {@code rollmod.warps.max} (integer) &rarr; per-player warp allowance.
 *   <li>Meta {@code rollmod.homes.max} (integer) &rarr; per-player home allowance.
 * </ul>
 */
public final class LuckPermsCompat {

  public static final boolean LOADED = ModList.get().isLoaded("luckperms");

  private static final String PRIME_PERMISSION = "rollmod.ah.prime";
  private static final String POSITIONS_META = "rollmod.ah.positions";
  private static final String WARPS_META = "rollmod.warps.max";
  private static final String MODERATE_PERMISSION = "rollmod.warps.moderate";
  private static final String HOMES_META = "rollmod.homes.max";

  private LuckPermsCompat() {}

  /** Max number of days {@code player} may list an item for. */
  public static int maxDurationDays(ServerPlayer player) {
    if (LOADED) {
      try {
        if (Lp.hasPermission(player, PRIME_PERMISSION)) {
          return Config.Server.AH_PRIME_DAYS.get();
        }
      } catch (Throwable ignored) {
        // LuckPerms not ready / user unloaded — fall through to default.
      }
    }
    return Config.Server.AH_MAX_DAYS.get();
  }

  /** Max number of simultaneous active listings {@code player} may hold. */
  public static int maxListings(ServerPlayer player) {
    if (LOADED) {
      try {
        Integer meta = Lp.metaInt(player, POSITIONS_META);
        if (meta != null) return Math.max(0, meta);
      } catch (Throwable ignored) {
        // fall through to default
      }
    }
    return Config.Server.AH_MAX_LISTINGS.get();
  }

  /**
   * Whether the player has the prime permission. Exposed because more than the auction house cares
   * about it now — the hub's warps grant a larger allowance to prime players.
   */
  public static boolean isPrime(ServerPlayer player) {
    if (!LOADED) return false;
    try {
      return Lp.hasPermission(player, PRIME_PERMISSION);
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * A rank's warp allowance from meta {@code rollmod.warps.max}, or {@code null} when unset — the
   * caller then falls back to its own prime/default pair. Numbers live in meta rather than in
   * permission nodes, per {@code LUCKPERMS_VALUE_PERMISSIONS.md}.
   */
  public static Integer warpLimit(ServerPlayer player) {
    if (!LOADED) return null;
    try {
      return Lp.metaInt(player, WARPS_META);
    } catch (Throwable ignored) {
      return null;
    }
  }

  /**
   * A rank's home allowance from meta {@code rollmod.homes.max}, or {@code null} when unset — the
   * caller then falls back to its own prime/default pair, exactly as {@link #warpLimit} is used.
   */
  public static Integer homeLimit(ServerPlayer player) {
    if (!LOADED) return null;
    try {
      return Lp.metaInt(player, HOMES_META);
    } catch (Throwable ignored) {
      return null;
    }
  }

  /**
   * Whether the player may act on warp reports. Lets a moderator rank that is not an operator still
   * be notified, which is what "moderation and higher" means on this server.
   */
  public static boolean canModerateWarps(ServerPlayer player) {
    if (!LOADED) return false;
    try {
      return Lp.hasPermission(player, MODERATE_PERMISSION);
    } catch (Throwable ignored) {
      return false;
    }
  }

  /** Isolated holder for the LuckPerms API; only referenced when {@link #LOADED}. */
  private static final class Lp {

    static boolean hasPermission(ServerPlayer player, String node) {
      net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
      net.luckperms.api.model.user.User user = lp.getUserManager().getUser(player.getUUID());
      if (user == null) return false;
      net.luckperms.api.query.QueryOptions options =
          lp.getContextManager()
              .getQueryOptions(user)
              .orElse(net.luckperms.api.query.QueryOptions.defaultContextualOptions());
      return user.getCachedData().getPermissionData(options).checkPermission(node).asBoolean();
    }

    static Integer metaInt(ServerPlayer player, String key) {
      net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
      net.luckperms.api.model.user.User user = lp.getUserManager().getUser(player.getUUID());
      if (user == null) return null;
      net.luckperms.api.query.QueryOptions options =
          lp.getContextManager()
              .getQueryOptions(user)
              .orElse(net.luckperms.api.query.QueryOptions.defaultContextualOptions());
      String value = user.getCachedData().getMetaData(options).getMetaValue(key);
      if (value == null) return null;
      try {
        return Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }
}
