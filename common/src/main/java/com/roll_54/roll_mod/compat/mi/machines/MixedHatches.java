package com.roll_54.roll_mod.compat.mi.machines;

import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.HatchMIHookContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixed hatches: item slots and one tank in a single hatch ({@link MixedHatch}), at IV. Ids are
 * {@code <tier>_mixed_input_hatch} / {@code <tier>_mixed_output_hatch}; models are written by
 * {@link com.roll_54.roll_mod.data.datagen.RollMachineModelProvider}.
 *
 * <p>Tiers, slot counts, tank sizes and layouts follow Yet Another Industrialization's mixed hatches (Luligabi1,
 * Apache License 2.0); YAI has the bronze to EV tiers, so only the IV one is here: 21 slots and the tank of this mod's
 * IV fluid hatch.
 */
public final class MixedHatches {

    /** Fits wherever an item or a fluid input hatch does. */
    public static final HatchType INPUT = HatchTypes.register(RollMod.id("mixed_input"),
            Component.translatable("hatch_type.roll_mod.mixed_input"));
    /** Fits wherever an item or a fluid output hatch does. */
    public static final HatchType OUTPUT = HatchTypes.register(RollMod.id("mixed_output"),
            Component.translatable("hatch_type.roll_mod.mixed_output"));

    /** The overlays, from Yet Another Industrialization. */
    public static final ResourceLocation OVERLAYS = RollMod.id("block/machines/mixed_hatch");
    public static final ResourceLocation OUTPUT_OVERLAY = RollMod.id("block/overlays/output_mixed");

    /**
     * @param tier             id prefix
     * @param englishTier      tier part of the English name
     * @param columns, rows    item slot grid
     * @param itemX, itemY     GUI position of the top-left item slot
     * @param buckets          tank capacity
     * @param fluidX, fluidY   GUI position of the tank
     * @param backgroundHeight GUI height (taller for three rows of slots)
     * @param casing           MI casing the hatch renders with
     */
    public record Spec(String tier, String englishTier, int columns, int rows, int itemX, int itemY, long buckets,
                       int fluidX, int fluidY, int backgroundHeight, String casing) {
        public String prefix() {
            return tier + "_mixed";
        }

        public List<String> ids() {
            return List.of(prefix() + "_input_hatch", prefix() + "_output_hatch");
        }
    }

    public static final Spec IV = new Spec("iv", "IV", 7, 3, 8, 28, FluidHatches.IV.buckets(), 152, 46, 176,
            FluidHatches.IV.casing());

    /**
     * IV only: Yet Another Industrialization ships the bronze to highly advanced (EV) ones itself; this mod adds the
     * tier above it (and the ME one, MeHatches#MIXED).
     */
    public static final List<Spec> ALL = List.of(IV);

    private MixedHatches() {}

    public static void register(HatchMIHookContext hook) {
        for (Spec spec : ALL) {
            hook.builder(spec.prefix(), spec.englishTier() + " Mixed")
                    .special((bep, input, id) -> create(spec, bep, input, id), true)
                    .registrator(MachineBlockEntity::registerItemApi)
                    .registrator(MachineBlockEntity::registerFluidApi)
                    .registerMachine();
        }
    }

    private static MixedHatch create(Spec spec, BEP bep, boolean input, ResourceLocation id) {
        List<ConfigurableItemStack> items = new ArrayList<>();
        for (int i = 0; i < spec.columns() * spec.rows(); i++) {
            items.add(input ? ConfigurableItemStack.standardInputSlot() : ConfigurableItemStack.standardOutputSlot());
        }
        long capacity = spec.buckets() * 1000L;
        List<ConfigurableFluidStack> fluids = List.of(input
                ? ConfigurableFluidStack.standardInputSlot(capacity)
                : ConfigurableFluidStack.standardOutputSlot(capacity));
        MIInventory inventory = new MIInventory(items, fluids,
                new SlotPositions.Builder().addSlots(spec.itemX(), spec.itemY(), spec.columns(), spec.rows()).build(),
                new SlotPositions.Builder().addSlot(spec.fluidX(), spec.fluidY()).build());
        MachineGuiParameters gui = new MachineGuiParameters.Builder(id, true)
                .backgroundHeight(spec.backgroundHeight()).build();
        return new MixedHatch(bep, gui, input, true, inventory);
    }
}
