package com.roll_54.roll_mod.compat.mi.machines;

import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.HatchMIHookContext;

import java.util.List;

/**
 * Fluid input/output hatches beyond MI's own tiers, registered under {@code roll_mod:} through
 * Tesseract (see {@link com.roll_54.roll_mod.compat.mi.RMMIRegistries}).
 *
 * <p>Tesseract names each pair {@code <prefix>_fluid_input_hatch} / {@code <prefix>_fluid_output_hatch}.
 * Models are written by {@link com.roll_54.roll_mod.data.datagen.RollMachineModelProvider} in the shape
 * MI ships for its own fluid hatches, so the builder's built-in model is left off.
 */
public final class FluidHatches {

    /**
     * @param prefix      id prefix, also the tier part of the English name
     * @param englishName tier part of the English name
     * @param buckets     capacity of the hatch's single tank
     * @param casing      MI casing the hatch renders with
     */
    public record Spec(String prefix, String englishName, int buckets, String casing) {
        public List<String> ids() {
            return List.of(prefix + "_fluid_input_hatch", prefix + "_fluid_output_hatch");
        }
    }

    /** One tier above MI's highly advanced (EV) hatches, which hold 64 buckets. */
    public static final Spec IV = new Spec("iv", "IV", 100_000, "superconductor");

    public static final List<Spec> ALL = List.of(IV);

    private FluidHatches() {}

    public static void register(HatchMIHookContext hook) {
        for (Spec spec : ALL) {
            hook.builder(spec.prefix(), spec.englishName())
                    .fluid(spec.buckets())
                    .registerIO()
                    .registerMachine();
        }
    }
}
