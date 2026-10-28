package com.roll_54.roll_mod.compat.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.parts.automation.ExportBusPart;
import appeng.parts.automation.IOBusPart;
import appeng.parts.automation.ImportBusPart;
import appeng.util.ConfigInventory;
import com.roll_54.roll_mod.economy.vendingblock.Config;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Decides whether an IO bus that just did no work did so because the vending block it faces blocks
 * it, in which case {@code IOBusPartMixin} lets the bus sleep until {@link Ae2BusWaker} wakes it.
 *
 * <p>Only the vending block's side is judged, never the network's: an export bus idle because its
 * items are not in the network yet must keep polling, as nothing on the vendor would wake it.
 */
public final class Ae2VendorBlocking {

  /** AdvancedAE's combined bus: exports through {@link ExportBusPart} and imports on top. */
  private static final String ADVANCED_AE_IMPORT_EXPORT =
      "net.pedroksl.advanced_ae.common.parts.ImportExportBusPart";

  private Ae2VendorBlocking() {}

  public static boolean blocks(IOBusPart bus) {
    if (!Config.Server.AE2_BUS_SLEEP_ON_VENDOR.get()) return false;
    Level level = bus.getLevel();
    BlockEntity host = bus.getBlockEntity();
    if (level == null || host == null) return false;
    Direction side = bus.getSide();
    BlockPos target = host.getBlockPos().relative(side);
    if (!(level.getBlockEntity(target) instanceof VendorBlockEntity)) return false;
    // The same query the bus's strategies make, so the same face handler: extract-only from below,
    // insert-only from every other side.
    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, side.getOpposite());
    if (handler == null) return false;

    if (bus instanceof ImportBusPart) return nothingToExtract(handler);
    if (bus instanceof ExportBusPart export) {
      boolean blocked = noConfiguredItemFits(export, handler);
      return export.getClass().getName().equals(ADVANCED_AE_IMPORT_EXPORT)
          ? blocked && nothingToExtract(handler)
          : blocked;
    }
    // Exporters outside ExportBusPart (ExtendedAE's mod and tag export buses) pick their items by
    // filter, not by a list we could test, so only a block nothing at all can enter counts.
    return nothingFits(handler, side.getOpposite());
  }

  private static boolean nothingToExtract(IItemHandler handler) {
    for (int i = 0; i < handler.getSlots(); i++) {
      if (!handler.extractItem(i, 1, true).isEmpty()) return false;
    }
    return true;
  }

  /** No item key in the bus's config fits; fluid keys never do, and an empty config exports nothing. */
  private static boolean noConfiguredItemFits(ExportBusPart bus, IItemHandler handler) {
    ConfigInventory config = bus.getConfig();
    for (int i = 0; i < config.size(); i++) {
      AEKey key = config.getKey(i);
      if (key instanceof AEItemKey item && ItemHandlerHelper.insertItem(handler, item.toStack(1), true).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /** The vendor's bottom face takes nothing; any other face takes something while a slot has room. */
  private static boolean nothingFits(IItemHandler handler, Direction face) {
    if (face == Direction.DOWN) return true;
    for (int i = 0; i < handler.getSlots(); i++) {
      ItemStack inSlot = handler.getStackInSlot(i);
      if (inSlot.isEmpty() || inSlot.getCount() < handler.getSlotLimit(i)) return false;
    }
    return true;
  }
}
