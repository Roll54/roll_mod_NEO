package com.roll_54.roll_mod.mixin;

import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.machines.guicomponents.RecipeEfficiencyBar;
import aztech.modern_industrialization.machines.init.SingleBlockCraftingMachines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.Consumer;

@Mixin(SingleBlockCraftingMachines.class)
public abstract class SingleBlockCraftingMachinesMixin {

    @ModifyArgs(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Laztech/modern_industrialization/machines/init/SingleBlockCraftingMachines;registerMachineTiers(Ljava/lang/String;Ljava/lang/String;Laztech/modern_industrialization/machines/recipe/MachineRecipeType;IIIILjava/util/function/Consumer;Laztech/modern_industrialization/machines/guicomponents/ProgressBar$Params;Laztech/modern_industrialization/machines/guicomponents/RecipeEfficiencyBar$Params;Laztech/modern_industrialization/machines/guicomponents/EnergyBar$Params;Ljava/util/function/Consumer;Ljava/util/function/Consumer;ZZZII)V"
            )
    )
    private static void modifyCentrifugeSlots(Args args) {
        String machineName = args.get(1);

        if ("centrifuge".equals(machineName)) {
            // -- ITEM SLOTS --
            args.set(4, 6); // itemOutputCount -> 6 (Total: 1 in + 6 out = 7)
            Consumer<SlotPositions.Builder> newItemsPositions = items -> items
                    .addSlot(42, 27) // 1 item input
                    .addSlots(93, 27, 2, 3); // 6 item outputs
            args.set(11, newItemsPositions);

            // -- FLUID SLOTS --
            args.set(5, 1); // fluidInputCount -> 1
            args.set(6, 6); // fluidOutputCount -> 6 (Total: 1 in + 6 out = 7)

            // -- GUI PARAMS --
            // Index 7 is guiParams
            Consumer<MachineGuiParameters.Builder> customGuiParams = builder -> builder.backgroundHeight(186);
            args.set(7, customGuiParams);

            // -- RECIPE EFFICIENCY BAR --
            // Index 9 is RecipeEfficiencyBar.Params
            args.set(9, new RecipeEfficiencyBar.Params(40, 86));


            Consumer<SlotPositions.Builder> newFluidPositions = fluids -> fluids
                    .addSlot(42, 45) // 1 fluid input position
                    .addSlots(131, 27, 2, 3); // 6 fluid output positions
            args.set(12, newFluidPositions);


            // -- MISC --
            args.set(17, 128); // ioBucketCapacity
        }
    }
}