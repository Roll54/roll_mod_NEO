package com.roll_54.roll_mod.mixin;

import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.init.MultiblockMachines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.Consumer;

/**
 * Extra item slots in the recipe viewer for the Electric Blast Furnace only. Its categories are built in
 * registerEbfReiCategories, one per coil tier, and every one of them only holds recipes above 4 EU/t —
 * the 4 EU/t and below recipes live in the Steam Blast Furnace category, which is left untouched.
 *
 * Layout follows MI's own spacing around the 20px arrow at (77, 33): item inputs end 3px left of it,
 * item outputs start 5px right of it, fluids sit 2px beyond the items, rows are centered on the arrow.
 */
@Mixin(MultiblockMachines.class)
public abstract class MutiblockMachinesMixin {

    @ModifyArgs(
            method = "registerEbfReiCategories()V",
            at = @At(
                    value = "INVOKE",
                    target = "Laztech/modern_industrialization/machines/init/MultiblockMachines$Rei;items(Ljava/util/function/Consumer;Ljava/util/function/Consumer;)Laztech/modern_industrialization/machines/init/MultiblockMachines$Rei;"
            )
    )
    private static void modifyElectricBlastFurnaceItemSlots(Args args) {
        Consumer<SlotPositions.Builder> inputs = builder -> builder.addSlots(38, 26, 2, 2);  // 2x2, x 38..74
        Consumer<SlotPositions.Builder> outputs = builder -> builder.addSlots(102, 35, 2, 1); // 2x1, x 102..138

        args.set(0, inputs);
        args.set(1, outputs);
    }

    @ModifyArgs(
            method = "registerEbfReiCategories()V",
            at = @At(
                    value = "INVOKE",
                    target = "Laztech/modern_industrialization/machines/init/MultiblockMachines$Rei;fluids(Ljava/util/function/Consumer;Ljava/util/function/Consumer;)Laztech/modern_industrialization/machines/init/MultiblockMachines$Rei;"
            )
    )
    private static void modifyElectricBlastFurnaceFluidSlots(Args args) {
        // Pushed outwards so they don't overlap the wider item grids.
        Consumer<SlotPositions.Builder> inputs = builder -> builder.addSlot(18, 35);
        Consumer<SlotPositions.Builder> outputs = builder -> builder.addSlot(140, 35);

        args.set(0, inputs);
        args.set(1, outputs);
    }
}
