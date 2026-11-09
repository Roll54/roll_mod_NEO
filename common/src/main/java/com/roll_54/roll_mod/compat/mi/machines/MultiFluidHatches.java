package com.roll_54.roll_mod.compat.mi.machines;

import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.hatches.FluidHatch;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import net.minecraft.resources.ResourceLocation;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.HatchMIHookContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fluid input/output hatches with several tanks, registered under {@code roll_mod:} through Tesseract.
 *
 * <p>Tesseract's {@code fluid(buckets)} builds MI's one-tank hatch, so these go through
 * {@code special(...)} with the same block entity, MI's own {@link FluidHatch}, given an inventory of
 * several tanks: it already hands every tank to the multiblock. Each tank holds what MI's one-tank hatch
 * of the same tier holds (steel 16 B, advanced 64 B, turbo 256 B, highly advanced 1024 B, and for IV this
 * mod's own {@link FluidHatches#IV}).
 *
 * <p>Ids are {@code <tier>_multi_fluid_input_hatch} / {@code <tier>_multi_fluid_output_hatch}. Models are
 * written by {@link com.roll_54.roll_mod.data.datagen.RollMachineModelProvider}.
 */
public final class MultiFluidHatches {

    /**
     * @param tier           id prefix
     * @param englishTier    tier part of the English name
     * @param columns        tank grid width in the GUI
     * @param rows           tank grid height in the GUI
     * @param bucketsPerTank capacity of each tank
     * @param slotX          GUI x of the top-left tank
     * @param slotY          GUI y of the top-left tank
     * @param casing         MI casing the hatch renders with
     */
    public record Spec(String tier, String englishTier, int columns, int rows, int bucketsPerTank, int slotX, int slotY,
                       String casing) {
        public int tanks() {
            return columns * rows;
        }

        public String prefix() {
            return tier + "_multi_fluid";
        }

        public List<String> ids() {
            return List.of(prefix() + "_input_hatch", prefix() + "_output_hatch");
        }
    }

    // Grid origins are those of MI's item hatch with the same grid (MultiblockHatches.registerItemHatches), so the
    // tanks sit exactly where that hatch's slots do: advanced 2x2 at (71, 30), turbo 3x3 at (62, 21), highly
    // advanced 5x3 at (44, 18). MI has no 2x1 hatch; LV is centred on MI's one-tank fluid slot (80, 40).
    public static final Spec LV = new Spec("lv", "LV", 2, 1, 16, 71, 40, "lv");
    public static final Spec MV = new Spec("mv", "MV", 2, 2, 64, 71, 30, "mv");
    public static final Spec HV = new Spec("hv", "HV", 3, 3, 256, 62, 21, "hv");
    public static final Spec EV = new Spec("ev", "EV", 5, 3, 1024, 44, 18, "ev");
    /** Same tank count and layout as EV; each tank holds what this mod's one-tank IV hatch holds. */
    public static final Spec IV = new Spec("iv", "IV", 5, 3, FluidHatches.IV.buckets(), 44, 18, FluidHatches.IV.casing());

    public static final List<Spec> ALL = List.of(LV, MV, HV, EV, IV);

    private MultiFluidHatches() {}

    public static void register(HatchMIHookContext hook) {
        for (Spec spec : ALL) {
            hook.builder(spec.prefix(), spec.englishTier() + " Multi Fluid")
                    .special((bep, input, id) -> create(spec, bep, input, id), true)
                    .registrator(MachineBlockEntity::registerFluidApi)
                    .registerMachine();
        }
    }

    private static FluidHatch create(Spec spec, BEP bep, boolean input, ResourceLocation id) {
        long capacity = spec.bucketsPerTank() * 1000L;
        List<ConfigurableFluidStack> tanks = new ArrayList<>(spec.tanks());
        for (int i = 0; i < spec.tanks(); i++) {
            tanks.add(input
                    ? ConfigurableFluidStack.standardInputSlot(capacity)
                    : ConfigurableFluidStack.standardOutputSlot(capacity));
        }
        // Slot positions are GUI layout only; they are not saved, so existing hatches just show the new layout.
        MIInventory inventory = new MIInventory(
                Collections.emptyList(), tanks, SlotPositions.empty(),
                new SlotPositions.Builder().addSlots(spec.slotX(), spec.slotY(), spec.columns(), spec.rows()).build());
        return new FluidHatch(bep, new MachineGuiParameters.Builder(id, true).build(), input, true, inventory);
    }
}
