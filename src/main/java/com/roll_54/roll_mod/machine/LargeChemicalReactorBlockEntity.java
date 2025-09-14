package com.roll_54.roll_mod.machine;

import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.init.MachineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.swedz.extended_industrialization.EI;
import com.roll_54.roll_mod.registry.ModBlockEntities;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.ElectricMultipliedCraftingMultiblockBlockEntity;

public class LargeChemicalReactorBlockEntity
        extends ElectricMultipliedCraftingMultiblockBlockEntity {

    public enum Mode { LCR, UCR }

    private Mode mode = Mode.LCR;
    // private MachineRecipeType<?> activeType; // розкоментуй, коли імпортуєш тип
    private int dynamicBatch = 128;

    public LargeChemicalReactorBlockEntity(BlockPos pos, BlockState state) {
        super(
                new BEP(ModBlockEntities.LARGE_CHEMICAL_REACTOR_BE.get(), pos, state),
                EI.id("large_chemical_reactor"),
                null, // TODO shape
                MachineTier.LV,
                MIMachineRecipeTypes.CHEMICAL_REACTOR,
                128,
                null // TODO EU transform
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

    private void setDynamicBatchSize(int b) { this.dynamicBatch = b; }
}
