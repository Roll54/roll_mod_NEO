package com.roll_54.roll_mod.hydroponics.gui;

import com.roll_54.roll_mod.hydroponics.HydroponicReagents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * One of the Hydroponic Garden Bed's three shared inputs. Accepts either a dry reagent registered in
 * the matching data map, or any fluid container holding a fluid that is.
 *
 * <p>Fluid containers are limited to one per slot. With only three slots there is nowhere else to
 * put an emptied bucket, so the drained container has to go back where it came from — which is only
 * ever legal if the slot was holding exactly one. Dry reagents keep their normal stack size.
 */
public class HydroponicInputSlot extends SlotItemHandler {

    private final HydroponicReagents.Kind kind;

    public HydroponicInputSlot(IItemHandler handler, HydroponicReagents.Kind kind, int x, int y) {
        super(handler, kind.slot, x, y);
        this.kind = kind;
    }

    public HydroponicReagents.Kind kind() {
        return kind;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return HydroponicReagents.accepts(kind, stack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return HydroponicReagents.fluidHandlerOf(stack) != null ? 1 : super.getMaxStackSize(stack);
    }
}
