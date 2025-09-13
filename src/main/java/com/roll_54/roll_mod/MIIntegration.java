package com.roll_54.roll_mod;

// Імпорт з MI/EI — підстав свої:
import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.machines.models.MachineCasings;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.machine.builder.SpecialMachineBuilder;
// import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;

public final class MIIntegration {
    private MIIntegration() {}

    public static void multiblocks(MultiblockMachinesMIHookContext hook) {
        // Проста форма 3×3×3 roofed
        SimpleMember wall = SimpleMember.forBlock(/* EIMachines.Casings.HEATPROOF */ MachineCasings.HEATPROOF); // TODO якщо інший casing
        HatchFlags hatches = new HatchFlags.Builder()
                .with(HatchTypes.ITEM_INPUT, HatchTypes.ITEM_OUTPUT, HatchTypes.FLUID_INPUT, HatchTypes.ENERGY_INPUT)
                .build();

        ShapeTemplate shape = new ShapeTemplate.Builder(/*EIMachines.Casings.HEATPROOF*/ MachineCasings.HEATPROOF)
                .add3by3LevelsRoofed(-1, 1, wall, hatches)
                .build();

        ((SpecialMachineBuilder)((SpecialMachineBuilder)
                hook.builder("large_chemical_reactor", "Large Chemical Reactor",
                                com.roll_54.roll_mod.machine.LargeChemicalReactorBlockEntity::new)
                        .builtinModel(MachineCasings.CLEAN_STAINLESS_STEEL, "large_chemical_reactor",
                                m -> m.front(true).active(true)))
                .registerMachine())
                .registerMultiblockShape(shape)
                .registerAsWorkstationFor(MI.id("chemical_reactor"))
                .registerAsWorkstationFor(MI.id("upgraded_chemical_reactor"));
    }
}
