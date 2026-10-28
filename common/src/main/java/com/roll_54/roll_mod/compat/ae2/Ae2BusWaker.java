package com.roll_54.roll_mod.compat.ae2;

import appeng.api.parts.IPartHost;
import appeng.parts.automation.IOBusPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Wakes the AE2 buses facing a block that {@link Ae2VendorBlocking} may have put to sleep.
 *
 * <p>Only call this behind {@code ModList.isLoaded("ae2")}: it names AE2 classes.
 */
public final class Ae2BusWaker {

  private Ae2BusWaker() {}

  /**
   * Alerts every IO bus mounted on a neighbouring cable bus and pointed at {@code pos}. {@code
   * alertDevice} moves a sleeping bus back to the awake queue and ticks it next tick; it does nothing
   * for the bus that is ticking right now, so a bus whose own transfer changed the block is not
   * re-queued against itself.
   */
  public static void wake(Level level, BlockPos pos) {
    for (Direction d : Direction.values()) {
      BlockPos neighbour = pos.relative(d);
      if (!level.isLoaded(neighbour)) continue;
      if (level.getBlockEntity(neighbour) instanceof IPartHost host
          && host.getPart(d.getOpposite()) instanceof IOBusPart bus) {
        bus.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
      }
    }
  }
}
