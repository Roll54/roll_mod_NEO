package com.roll_54.roll_mod.economy.vendingblock.block.events;

import com.roll_54.roll_mod.economy.vendingblock.Config;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.OwnedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class BreakEvent {

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
    Level level = event.getLevel();
    BlockPos pos = event.getPos();
    Player player = event.getEntity();

    OwnedBlockEntity owned = OwnedBlockEntity.at(level, pos);
    if (owned != null && !canBreak(player, owned)) {
      event.setCanceled(true);
    }
  }

  private static boolean canBreak(Player player, OwnedBlockEntity blockEntity) {

    Config.Server.BreakLevel breakLevel = Config.Server.BREAK_LEVEL.get();

    if (blockEntity.isOwner(player)) return true;

    switch (breakLevel) {
      case SERVER_OWNER:
        return isServerOwner(player);
      case ADMIN:
        return isAdmin(player) || isServerOwner(player);
      case GAMEMASTER:
        return isGamemaster(player) || isAdmin(player) || isServerOwner(player);
      case MODERATOR:
        return isModerator(player)
            || isGamemaster(player)
            || isAdmin(player)
            || isServerOwner(player);
      case BLOCK_OWNER_ONLY:
        return false;
      default:
        return false;
    }
  }

  private static boolean isServerOwner(Player player) {
    return player.hasPermissions(4);
  }

  private static boolean isAdmin(Player player) {
    return player.hasPermissions(3);
  }

  private static boolean isGamemaster(Player player) {
    return player.hasPermissions(2);
  }

  private static boolean isModerator(Player player) {
    return player.hasPermissions(1);
  }
}
