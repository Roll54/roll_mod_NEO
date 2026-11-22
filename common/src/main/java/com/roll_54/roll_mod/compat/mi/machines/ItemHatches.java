package com.roll_54.roll_mod.compat.mi.machines;

import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.HatchMIHookContext;

import java.util.List;

/**
 * Item input/output hatches beyond MI's own tiers, registered under {@code roll_mod:} through Tesseract, as
 * {@link FluidHatches} does for fluids. Ids are {@code <prefix>_item_input_hatch} / {@code <prefix>_item_output_hatch};
 * models are written by {@link com.roll_54.roll_mod.data.datagen.RollMachineModelProvider} in the shape MI ships for
 * its own item hatches.
 *
 * @param rows    slot grid height in the GUI
 * @param columns slot grid width in the GUI
 * @param slotX   GUI x of the top-left slot
 * @param slotY   GUI y of the top-left slot
 * @param casing  MI casing the hatch renders with
 */
public final class ItemHatches {

    public record Spec(String prefix, String englishName, int rows, int columns, int slotX, int slotY, String casing) {
        public List<String> ids() {
            return List.of(prefix + "_item_input_hatch", prefix + "_item_output_hatch");
        }
    }

    /**
     * One tier above MI's highly advanced (EV) hatches, which hold 5x3: 8x3, centred in the same window, on the
     * same casing as this mod's IV fluid hatches.
     */
    public static final Spec IV = new Spec("iv", "IV", 3, 8, 17, 18, FluidHatches.IV.casing());

    public static final List<Spec> ALL = List.of(IV);

    private ItemHatches() {}

    public static void register(HatchMIHookContext hook) {
        for (Spec spec : ALL) {
            hook.builder(spec.prefix(), spec.englishName())
                    .item(spec.rows(), spec.columns(), spec.slotX(), spec.slotY())
                    .registerIO()
                    .registerMachine();
        }
    }
}
