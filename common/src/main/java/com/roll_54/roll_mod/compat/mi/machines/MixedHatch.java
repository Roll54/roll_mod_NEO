package com.roll_54.roll_mod.compat.mi.machines;

import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.components.OrientationComponent;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.machines.guicomponents.AutoExtract;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.HatchType;

import java.util.List;

/**
 * A hatch with item slots and a tank together, handing both to the multiblock: items as item inputs (or outputs),
 * the tank as a fluid input (or output). Its own hatch type ({@link MixedHatches#INPUT} / {@link MixedHatches#OUTPUT})
 * fits wherever an item or a fluid hatch of the same direction does ({@code HatchFlagsMixedMixin}).
 *
 * <p>Ported from Yet Another Industrialization's {@code MixedHatch} (Luligabi1, Apache License 2.0).
 */
public class MixedHatch extends HatchBlockEntity {

    private final boolean input;
    private final boolean upgradesToSteel;
    private final MIInventory inventory;

    public MixedHatch(BEP bep, MachineGuiParameters guiParams, boolean input, boolean upgradesToSteel, MIInventory inventory) {
        super(bep, guiParams, new OrientationComponent.Params(true, true, true));
        this.input = input;
        this.upgradesToSteel = upgradesToSteel;
        this.inventory = inventory;
        registerComponents(inventory);
        registerGuiComponent(new AutoExtract(orientation, input));
    }

    @Override
    public void appendItemInputs(List<ConfigurableItemStack> list) {
        if (input) list.addAll(inventory.getItemStacks());
    }

    @Override
    public void appendItemOutputs(List<ConfigurableItemStack> list) {
        if (!input) list.addAll(inventory.getItemStacks());
    }

    @Override
    public void appendFluidInputs(List<ConfigurableFluidStack> list) {
        if (input) list.addAll(inventory.getFluidStacks());
    }

    @Override
    public void appendFluidOutputs(List<ConfigurableFluidStack> list) {
        if (!input) list.addAll(inventory.getFluidStacks());
    }

    @Override
    protected void tickTransfer() {
        if (orientation.extractItems) {
            if (input) inventory.autoInsertItems(level, worldPosition, orientation.outputDirection);
            else inventory.autoExtractItems(level, worldPosition, orientation.outputDirection);
        }
        if (orientation.extractFluids) {
            if (input) inventory.autoInsertFluids(level, worldPosition, orientation.outputDirection);
            else inventory.autoExtractFluids(level, worldPosition, orientation.outputDirection);
        }
    }

    @Override
    public HatchType getHatchType() {
        return input ? MixedHatches.INPUT : MixedHatches.OUTPUT;
    }

    @Override
    public boolean upgradesToSteel() {
        return upgradesToSteel;
    }

    @Override
    public MIInventory getInventory() {
        return inventory;
    }
}
