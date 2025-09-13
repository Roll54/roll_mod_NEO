package com.roll_54.roll_mod.machine;

import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.init.MachineTier;
import com.roll_54.roll_mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.swedz.extended_industrialization.EI;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.ElectricMultipliedCraftingMultiblockBlockEntity;

public class LargeChemicalReactorBlockEntity
        extends ElectricMultipliedCraftingMultiblockBlockEntity {

    public enum Mode { LCR, UCR }

    private Mode mode = Mode.LCR;
    // private MachineRecipeType<?> activeType; // розкоментуй, коли імпортуєш тип
    private int dynamicBatch = 128;

    public LargeChemicalReactorBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.LARGE_CHEMICAL_REACTOR_BE.get(),
                pos, state,
                EI.id("large_chemical_reactor"),
 /* new ShapeTemplate[] { ... } */ null,     // TODO
         MachineTier.LV,
  MIMachineRecipeTypes.CHEMICAL_REACTOR,
  128,
                /* EU transform  */ /* EuCostTransformers.none() */ null        // TODO
        );
        updateActiveType();
    }

    /* ================= РЕЖИМИ ================= */

    public void cycleMode() {
        setMode(this.mode == Mode.LCR ? Mode.UCR : Mode.LCR);
    }

    public void setMode(Mode m) {
        if (this.mode != m) {
            this.mode = m;
            updateActiveType();
            cancelCurrentRecipeIfAny();
            setChanged();
            sync(); // якщо маєш власну синхронізацію
        }
    }

    public Mode getMode() { return mode; }
    public int getModeInt() { return mode == Mode.LCR ? 0 : 1; }
    public void setModeInt(int v) { setMode(v == 0 ? Mode.LCR : Mode.UCR); }

    private void updateActiveType() {
        if (mode == Mode.LCR) {
            // this.activeType = MIMachineRecipeTypes.CHEMICAL_REACTOR;
            this.dynamicBatch = 128;
        } else {
            // this.activeType = MITweaksRecipeTypes.UPGRADED_CHEMICAL_REACTOR; // з MI-Tweaks
            this.dynamicBatch = computeUcrBatchFromCoils();
        }
    }

    /* =========== КОТУШКИ ⇒ batch (UCR) =========== */

    private int computeUcrBatchFromCoils() {
        CoilTier coil = getInstalledCoilTier();
        if (coil == CoilTier.KANTHAL) return 16;
        if (coil == CoilTier.CUPRONICKEL) return 4;
        return 1;
    }

    private enum CoilTier { CUPRONICKEL, KANTHAL, UNKNOWN }

    private CoilTier getInstalledCoilTier() {
        // TODO: зчитати coil із вашого компонента/shape (теги або компонент типу CoilsComponent)
        return CoilTier.UNKNOWN;
    }

    /* =========== ПОШУК РЕЦЕПТУ =========== */

    @Override
    protected java.util.Optional/*<? extends MachineRecipe>*/ findMatchingRecipe() {
        updateActiveType();
        setDynamicBatchSize(this.dynamicBatch);
        // return findRecipeInType(activeType);
        return java.util.Optional.empty(); // TODO: поверни реальний пошук
    }

    private void setDynamicBatchSize(int b) { this.dynamicBatch = b; }

    @Override
    protected int getConfiguredBatchSize() { return dynamicBatch; }

    /* =========== NBT =========== */

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("mode", getModeInt());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("mode")) setModeInt(tag.getInt("mode"));
    }
}
