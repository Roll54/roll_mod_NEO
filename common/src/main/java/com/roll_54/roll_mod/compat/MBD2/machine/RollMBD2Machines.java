package com.roll_54.roll_mod.compat.MBD2.machine;

import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.MBD2.crops.CropHarvesterTraitDefinition;
import com.roll_54.roll_mod.compat.MBD2.energy.MIEnergyTraitDefinition;
import com.roll_54.roll_mod.registry.TagRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

/**
 * Registers this mod's MBD2 machines.
 *
 * <p>MBD2 normally loads machines from {@code .sm} project files written by its in-game editor. This
 * one is assembled in Java instead, so it lives in git, needs no editor round-trip and cannot drift
 * from the {@link CropHarvesterTraitDefinition} it depends on. The trade-off is that a Java-built
 * definition is not a project file, so the F4 editor cannot edit it in place. Because every widget is
 * keyed by {@code IUIProviderTrait#uiId()} (see {@link CropManagerUI}), swapping to an editor-authored
 * layout later is a one-line change:
 * {@code event.registerFromResource(getClass(), RollMod.MODID, "machine/crop_manager_mk2.sm")}.
 *
 * <p>Registered on the mod event bus from {@code RollMod}'s constructor. Note that MBD2 fires this
 * from {@code FMLConstructModEvent} — <em>before</em> {@code RegisterEvent} — so nothing here may
 * resolve a {@code DeferredHolder}. That is why the herbicide slot filters on an item <em>tag</em>
 * rather than on item instances.
 */
public class RollMBD2Machines {

    public static final ResourceLocation CROP_MANAGER_MK2 = RollMod.id("crop_manager_mk2");

    /** Same buffer and throughput as the deprecated Crop Manager, so balance is unchanged. */
    private static final int ENERGY_CAPACITY = 100_000;
    private static final int ENERGY_TRANSFER = 1_000;

    private static final String HERBICIDE_TRAIT = "herbicide";
    private static final String OUTPUT_TRAIT = "output";
    private static final String ENERGY_TRAIT = "energy";

    private static final int OUTPUT_SLOTS = 18;

    @SubscribeEvent
    public void onRegisterMachines(MBDRegistryEvent.Machine event) {
        event.register(cropManagerMk2());
        RollMod.LOGGER.info("[{}] registered MBD2 machine '{}'", RollMod.MODID, CROP_MANAGER_MK2);
    }

    private static MBDMachineDefinition cropManagerMk2() {
        return MBDMachineDefinition.builder()
                .id(CROP_MANAGER_MK2)
                .rootState(rootState())
                .blockProperties(ConfigBlockProperties.builder()
                        // A horizontal facing is what gives "behind the machine" a meaning.
                        .rotationState(RotationState.NON_Y_AXIS)
                        .destroyTime(3.0f)
                        .explosionResistance(9.0f)
                        .build())
                // The factory is re-invoked per definition load, so each call builds fresh traits.
                .machineSettings(RollMBD2Machines::cropManagerSettings)
                // Explicitly off: ConfigRecipeLogicSettings defaults enable=true, which would leave
                // RecipeLogic ticking against a machine that has no recipe type at all. The harvester
                // trait does the work from serverTick() instead.
                .recipeLogicSettings(ConfigRecipeLogicSettings.builder().enable(false).build())
                .build();
    }

    /**
     * Idle and active looks, one model each.
     *
     * <p>Both models are authored facing NORTH with a distinct front (north) and back (south) face;
     * MBD2 bakes a rotated copy per facing and caches it ({@code MBDMachineBlock.getModelState} ->
     * {@code ModelFactory.getRotation}), so no blockstate file and no per-facing variants are needed.
     *
     * <p>A child state with no renderer of its own inherits its parent's, so {@code waiting} follows
     * {@code working} into the active model and {@code suspend} stays on the idle one.
     */
    private static MachineState rootState() {
        @SuppressWarnings("unchecked")
        MachineState.Builder<MachineState> builder =
                (MachineState.Builder<MachineState>) MachineState.baseBuilder();
        return builder
                .modelRenderer(RollMod.id("block/crop_manager_mk2"))
                .child("working", working -> working
                        .modelRenderer(RollMod.id("block/crop_manager_mk2_active"))
                        .child("waiting"))
                .child("suspend")
                .build();
    }

    /**
     * The machine's traits and its default GUI. No recipe logic: {@link CropHarvesterTraitDefinition}
     * drives the machine directly from {@code serverTick()}, so there is no recipe type to match.
     *
     * <p>Trait <em>names</em> are load-bearing twice over — the harvester looks its siblings up by
     * name, and every GUI widget id is derived from the same name.
     */
    private static ConfigMachineSettings cropManagerSettings() {
        ItemSlotCapabilityTraitDefinition herbicide = new ItemSlotCapabilityTraitDefinition();
        herbicide.setName(HERBICIDE_TRAIT);
        herbicide.setSlotSize(1);
        herbicide.setGuiIO(IO.IN);
        herbicide.getCapabilityIO().setAllIO(IO.IN);
        // Tag, not item instances: this runs during mod construction, before items exist.
        herbicide.getItemFilterSettings().setEnable(true);
        herbicide.getItemFilterSettings().setWhitelist(true);
        herbicide.getItemFilterSettings().setFilterTags(List.of(TagRegistry.HERBICIDES.location()));

        ItemSlotCapabilityTraitDefinition output = new ItemSlotCapabilityTraitDefinition();
        output.setName(OUTPUT_TRAIT);
        output.setSlotSize(OUTPUT_SLOTS);
        output.setGuiIO(IO.OUT);
        // Capability only, no auto-IO: a hopper or pipe on any face can pull from the grid, exactly
        // as it could from the old block's SidedInvWrapper. Auto-IO would instead actively push items
        // into whatever container happens to sit next to the machine, which nobody asked for.
        output.getCapabilityIO().setAllIO(IO.OUT);

        MIEnergyTraitDefinition energy = new MIEnergyTraitDefinition();
        energy.setName(ENERGY_TRAIT);
        energy.setCapacity(ENERGY_CAPACITY);
        energy.setMaxReceive(ENERGY_TRANSFER);
        // Not 0: MIEnergyStore extends NeoForge's EnergyStorage, whose extractEnergy is clamped by
        // maxExtract — and that is the same call the harvester pays its operating cost with. External
        // drain is stopped by the IO.IN capability gate instead (see MIEnergyStorageWrapper#extract).
        energy.setMaxExtract(ENERGY_TRANSFER);
        energy.setCableTier("lv");
        energy.getCapabilityIO().setAllIO(IO.IN);

        CropHarvesterTraitDefinition harvester = new CropHarvesterTraitDefinition();
        harvester.setName("crop_harvester");
        harvester.setHerbicideSlotName(HERBICIDE_TRAIT);
        harvester.setOutputSlotName(OUTPUT_TRAIT);
        harvester.setEnergyTraitName(ENERGY_TRAIT);

        List<TraitDefinition> traits = List.of(herbicide, output, energy, harvester);

        return ConfigMachineSettings.builder()
                .hasUI(true)
                .uiTemplate(CropManagerUI.template(output, herbicide, energy, harvester))
                .traitDefinitions(traits)
                .build();
    }
}
