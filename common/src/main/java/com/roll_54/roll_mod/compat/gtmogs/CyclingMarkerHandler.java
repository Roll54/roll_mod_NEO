package com.roll_54.roll_mod.compat.gtmogs;

import com.quantumgarbage.gtmogs.api.transfer.item.CustomItemStackHandler;
import net.minecraft.world.item.ItemStack;

/**
 * The dimension markers of one vein on its EMI/JEI page. Slot 0 shows each marker in turn, a second
 * apiece; the other slots hold them as usual. Used by {@code OreVeinDimensionCycleMixin}, outside the
 * mixin package because mixin classes cannot be instantiated.
 */
public class CyclingMarkerHandler extends CustomItemStackHandler {

    private static final long PERIOD_MS = 1000L;

    public CyclingMarkerHandler(int size) {
        super(size);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int count = getSlots();
        if (slot != 0 || count < 2) return super.getStackInSlot(slot);
        return super.getStackInSlot((int) (System.currentTimeMillis() / PERIOD_MS % count));
    }
}
