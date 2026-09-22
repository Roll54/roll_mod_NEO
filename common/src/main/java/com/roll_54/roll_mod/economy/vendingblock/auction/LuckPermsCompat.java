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
 *   <li>Permission {@code rollmod.warps.bypasswarmup} &rarr; warps travel without the warm-up.
 *   <li>Permission {@code rollmod.kit.<name>} &rarr; may claim that kit.
 *   <li>Permission {@code rollmod.rtp.bypasscooldown} &rarr; no wait between random teleports.
 *   <li>Meta {@code rollmod.rtp.cooldown} (ticks) &rarr; per-player wait between random teleports.
 *   <li>Permission {@code rollmod.fly.use} &rarr; may use {@code /fly}.
 *   <li>Permission {@code rollmod.heal.use} &rarr; may use {@code /heal}.
 *   <li>Permission {@code rollmod.heal.bypasscooldown} &rarr; no wait between heals.
 *   <li>Meta {@code rollmod.heal.cooldown} (ticks) &rarr; per-player wait between heals.
 * </ul>
 */
public final class LuckPermsCompat {

  public static final boolean LOADED = ModList.get().isLoaded("luckperms");

  private static final String PRIME_PERMISSION = "rollmod.ah.prime";
  private static final String POSITIONS_META = "rollmod.ah.positions";
  private static final String WARPS_META = "rollmod.warps.max";
  private static final String MODERATE_PERMISSION = "rollmod.warps.moderate";
  private static final String BYPASS_WARMUP_PERMISSION = "rollmod.warps.bypasswarmup";
  private static final String KIT_PERMISSION_PREFIX = "rollmod.kit.";
  private static final String RTP_BYPASS_PERMISSION = "rollmod.rtp.bypasscooldown";
  private static final String RTP_COOLDOWN_META = "rollmod.rtp.cooldown";
  private static final String HOMES_META = "rollmod.homes.max";
  private static final String FLY_PERMISSION = "rollmod.fly.use";
  private static final String HEAL_PERMISSION = "rollmod.heal.use";
  private static final String HEAL_BYPASS_PERMISSION = "rollmod.heal.bypasscooldown";
  private static final String HEAL_COOLDOWN_META = "rollmod.heal.cooldown";

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

  /**
   * Whether the player's warps skip the warm-up and travel at once. A rank perk, so it is a plain
   * boolean node rather than meta: there is no number to carry.
   */
  public static boolean canBypassWarmup(ServerPlayer player) {
    if (!LOADED) return false;
    try {
      return Lp.hasPermission(player, BYPASS_WARMUP_PERMISSION);
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * Whether the player may claim the named kit, through {@code rollmod.kit.<name>}.
   *
   * <p>Without LuckPerms nobody has any node, which would hide every kit, so an operator still gets
   * them: a single-player world or a server without the mod is not a place to enforce ranks.
   */
  public static boolean canUseKit(ServerPlayer player, String kit) {
    if (!LOADED) return player.hasPermissions(2);
    try {
      return Lp.hasPermission(player, KIT_PERMISSION_PREFIX + kit.toLowerCase(java.util.Locale.ROOT));
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * Whether the wait between random teleports is waived for this player.
   *
   * <p>Random teleports only. There is deliberately no equivalent for kits: a kit cooldown is the
   * whole of what makes a kit worth anything, so nothing waives it and nothing overrides it — its
   * value comes from the kit file and from nowhere else.
   */
  public static boolean canBypassRtpCooldown(ServerPlayer player) {
    if (!LOADED) return false;
    try {
      return Lp.hasPermission(player, RTP_BYPASS_PERMISSION);
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * A rank's wait between random teleports in <em>ticks</em> from meta {@code rollmod.rtp.cooldown},
   * or {@code null} when unset — the caller then falls back to the config default.
   *
   * <p>Ticks, not seconds, because that is the unit every other duration in the mod uses; only the
   * stamps written to disk are epoch millis.
   */
  public static Integer rtpCooldownTicks(ServerPlayer player) {
    return meta(player, RTP_COOLDOWN_META);
  }

  /** Whether the player may use {@code /fly}. */
  public static boolean canFly(ServerPlayer player) {
    return gated(player, FLY_PERMISSION);
  }

  /** Whether the player may use {@code /heal}. */
  public static boolean canHeal(ServerPlayer player) {
    return gated(player, HEAL_PERMISSION);
  }

  /**
   * Whether the wait between heals is waived for this player. A perk on top of a perk, so unlike
   * {@link #canHeal} it grants nothing of its own: a rank still needs {@code rollmod.heal.use}.
   */
  public static boolean canBypassHealCooldown(ServerPlayer player) {
    if (!LOADED) return false;
    try {
      return Lp.hasPermission(player, HEAL_BYPASS_PERMISSION);
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * A rank's wait between heals in <em>ticks</em> from meta {@code rollmod.heal.cooldown}, or
   * {@code null} when unset — the caller then falls back to the config, exactly as
   * {@link #rtpCooldownTicks} is used.
   */
  public static Integer healCooldownTicks(ServerPlayer player) {
    return meta(player, HEAL_COOLDOWN_META);
  }

  /**
   * A node that decides whether a command may be run at all.
   *
   * <p>Without LuckPerms nobody holds any node, which would refuse everybody, so an operator still
   * gets it — the same rule {@link #canUseKit} follows, and for the same reason: a single-player
   * world, or a server without the mod, is not a place to enforce ranks. Where LuckPerms is
   * installed the node is the whole of the answer, and being an operator grants nothing.
   */
  private static boolean gated(ServerPlayer player, String node) {
    if (!LOADED) return player.hasPermissions(2);
    try {
      return Lp.hasPermission(player, node);
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static Integer meta(ServerPlayer player, String key) {
    if (!LOADED) return null;
    try {
      Integer value = Lp.metaInt(player, key);
      return value == null ? null : Math.max(0, value);
    } catch (Throwable ignored) {
      return null;
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
