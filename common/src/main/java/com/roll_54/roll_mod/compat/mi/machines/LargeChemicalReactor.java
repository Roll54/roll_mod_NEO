package com.roll_54.roll_mod.compat.mi.machines;

import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.compat.rei.machines.SteamMode;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineComponent;
import aztech.modern_industrialization.machines.guicomponents.ShapeSelection;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.init.MachineTier;
import aztech.modern_industrialization.machines.models.MachineCasings;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.MachineRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformers;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.ElectricMultipliedCraftingMultiblockBlockEntity;

import java.util.List;

import static aztech.modern_industrialization.machines.multiblocks.HatchTypes.*;

/**
 * The pack's MI Tweaks batch chemical reactor ({@code mi_tweaks:large_chemical_reactor}), rebuilt under
 * {@code roll_mod:} with the same 3×3×3 PTFE shape, ×128 parallels and half EU per parallel, plus a recipe
 * type of its own, {@code modern_industrialization:large_chemical_reactor}. A switch in its screen (the
 * one MI's distillation tower uses for its height) picks what it runs: the chemical reactor's recipes, or
 * its own.
 *
 * <p>Running chemical reactor recipes it keeps MI Tweaks' single-block EU limit, 32 EU/t; its own recipes
 * have no limit. It shows in every chemical reactor category and has its own category for its own type.
 */
public final class LargeChemicalReactor {

    public static final String ID = "large_chemical_reactor";
    public static final String ENGLISH_NAME = "Large Chemical Reactor";
    /** PTFE casing, under the name {@code MachineRegistry.blockImitationCasings} registers. */
    public static final String CASING = "inert_casing";
    public static final ResourceLocation OVERLAYS =
            ResourceLocation.parse("roll_mod:block/machines/large_chemical_reactor");
    private static final int PARALLEL = 128;
    /** MI Tweaks ran it at single-block EU (32 EU/t) for chemical reactor recipes. */
    private static final long CHEMICAL_REACTOR_MAX_EU = 32;

    private static final String[][] LAYERS = {
            { "HHH", "MMM", "HHH" },
            { "HHH", "M M", "HHH" },
            { "HHH", "M#M", "HHH" },
    };
    private static final HatchFlags HATCHES = new HatchFlags.Builder()
            .with(ITEM_INPUT, ITEM_OUTPUT, FLUID_INPUT, FLUID_OUTPUT, ENERGY_INPUT).build();

    /** The two modes, in the order the switch shows them. */
    private enum Mode {
        CHEMICAL_REACTOR, OWN;

        MachineRecipeType recipeType() {
            return this == CHEMICAL_REACTOR ? MIMachineRecipeTypes.CHEMICAL_REACTOR : MachineRegistry.RecipeTypes.LARGE_CHEMICAL_REACTOR;
        }
    }

    private static ShapeTemplate shape;
    private static boolean registered;

    private LargeChemicalReactor() {}

    /** Call from the multiblock hook, after the recipe types are bound. */
    public static void register(MultiblockMachinesMIHookContext hook) {
        MachineRecipeType own = MachineRegistry.RecipeTypes.LARGE_CHEMICAL_REACTOR;
        if (own == null) {
            RollMod.LOGGER.warn("[MI] Skipping multiblock '{}', its recipe type is missing.", ID);
            return;
        }
        SimpleMember ptfe = SimpleMember.forBlockId(ResourceLocation.parse("modern_industrialization:chemically_inert_ptfe_casing"));
        shape = new ShapeTemplate.LayeredBuilder(MachineCasings.get(CASING), LAYERS)
                .key('H', ptfe, HATCHES)
                .key('M', ptfe, HatchFlags.NO_HATCH)
                .build();
        hook.builder(ID, ENGLISH_NAME, BlockEntity::new)
                // The category of its own type; the chemical reactor's are joined in registerWorkstations.
                .gui(true, SteamMode.ELECTRIC_ONLY, own, gui -> gui
                        .progressBar(77, 35, "arrow")
                        .slots(slots -> slots.append(
                                in -> in.addSlots(36, 17, 1, 3),
                                out -> out.addSlots(102, 17, 1, 3),
                                fluidIn -> fluidIn.addSlots(16, 17, 1, 3),
                                fluidOut -> fluidOut.addSlots(122, 17, 1, 3))))
                .registerMachine()
                .registerMultiblockShape(shape)
                .registerRecipeCategory();
        registered = true;
    }

    /** A workstation in every chemical reactor category. Call from common setup. */
    public static void registerWorkstations() {
        if (!registered) return;
        ResourceLocation machine = RollMod.id(ID);
        ReiMachineRecipes.categories.values().stream()
                .filter(params -> params.recipeType == MIMachineRecipeTypes.CHEMICAL_REACTOR)
                .map(params -> params.category)
                .toList()
                .forEach(category -> ReiMachineRecipes.registerWorkstation(category, machine));
    }

    private static final class BlockEntity extends ElectricMultipliedCraftingMultiblockBlockEntity {

        private Mode mode = Mode.CHEMICAL_REACTOR;

        BlockEntity(BEP bep) {
            super(bep, RollMod.id(ID), new ShapeTemplate[] { shape }, MachineTier.MULTIBLOCK,
                    MIMachineRecipeTypes.CHEMICAL_REACTOR, PARALLEL, EuCostTransformers.percentage(() -> 0.5f));
            registerComponents(new MachineComponent() {
                @Override
                public void writeNbt(CompoundTag tag, HolderLookup.Provider registries) {
                    tag.putInt("roll_mod_mode", mode.ordinal());
                }

                @Override
                public void readNbt(CompoundTag tag, HolderLookup.Provider registries, boolean isUpgradingMachine) {
                    int index = tag.getInt("roll_mod_mode");
                    mode = index >= 0 && index < Mode.values().length ? Mode.values()[index] : Mode.CHEMICAL_REACTOR;
                }
            });
            List<Component> names = List.of(
                    Component.translatable("gui.roll_mod.large_chemical_reactor.mode.chemical_reactor"),
                    Component.translatable("gui.roll_mod.large_chemical_reactor.mode.own"));
            registerGuiComponent(new ShapeSelection(new ShapeSelection.Behavior() {
                @Override
                public void handleClick(int clickedLine, int delta) {
                    int count = Mode.values().length;
                    mode = Mode.values()[Math.floorMod(mode.ordinal() + delta, count)];
                    setChanged();
                }

                @Override
                public int getCurrentIndex(int line) {
                    return mode.ordinal();
                }
            }, new ShapeSelection.LineInfo(names, false)));
        }

        @Override
        public MachineRecipeType getRecipeType() {
            // Null while the super constructor runs, before this class's fields are set.
            MachineRecipeType type = mode == null ? null : mode.recipeType();
            return type != null ? type : MIMachineRecipeTypes.CHEMICAL_REACTOR;
        }

        @Override
        public boolean isRecipeBanned(long recipeEu) {
            return (mode == Mode.CHEMICAL_REACTOR && recipeEu > CHEMICAL_REACTOR_MAX_EU) || super.isRecipeBanned(recipeEu);
        }
    }
}
