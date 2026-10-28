package com.roll_54.roll_mod.economy.service;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Experience as a whole number of points. Vanilla keeps a level plus a float progress, and
 * {@code giveExperiencePoints} with a negative amount drifts as it walks levels down; the XP services
 * read the exact total and write it back from zero instead.
 */
public final class ServiceXp {

  private ServiceXp() {}

  /** Points needed to reach {@code level} from zero (the vanilla curve). */
  public static long pointsForLevel(int level) {
    long l = Math.max(0, level);
    if (l <= 16) return l * l + 6 * l;
    if (l <= 31) return (5 * l * l - 81 * l + 720) / 2;
    return (9 * l * l - 325 * l + 4440) / 2;
  }

  /** The player's total points: whole levels plus the progress into the next one. */
  public static long totalPoints(Player player) {
    return pointsForLevel(player.experienceLevel)
        + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
  }

  /** Points to go from the player's current total up to exactly {@code levels} more levels. */
  public static long pointsToGain(Player player, int levels) {
    return Math.max(0L, pointsForLevel(player.experienceLevel + levels) - totalPoints(player));
  }

  /** Points to drop down to exactly {@code levels} fewer levels (no progress left over). */
  public static long pointsToLose(Player player, int levels) {
    return Math.max(0L, totalPoints(player) - pointsForLevel(player.experienceLevel - levels));
  }

  /** Sets the total exactly: levels and progress rebuilt from zero, then synced to the client. */
  public static void setTotalPoints(ServerPlayer player, long points) {
    player.experienceLevel = 0;
    player.experienceProgress = 0.0F;
    player.totalExperience = 0;
    player.giveExperiencePoints((int) Math.max(0L, Math.min(points, Integer.MAX_VALUE)));
  }
}
