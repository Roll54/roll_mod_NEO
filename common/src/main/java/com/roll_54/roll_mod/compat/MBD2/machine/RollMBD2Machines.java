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

    /**
     * One Crop Manager per voltage tier. The tiers differ in reach, buffer, intake and the cable
     * they take; the harvest itself — energy per crop, per weed, tick rate — is the same on all.
     *
     * <p>MV keeps the id {@code crop_manager_mk2}: it is the machine that existed before the tiers
     * did, so placed blocks keep working. It also keeps the deprecated Crop Manager's buffer and
     * throughput.
     *
     * @param area the side of the square work area behind the machine, in blocks
     */
    public enum CropManagerTier {
        LV("crop_manager_lv", "lv", 3, 50_000, 500),
        MV("crop_manager_mk2", "mv", 5, 100_000, 1_000),
        HV("crop_manager_hv", "hv", 7, 200_000, 2_000);

        public final ResourceLocation id;
        private final String cableTier;
        private final int area;
        private final int energyCapacity;
        private final int energyTransfer;

        CropManagerTier(String path, String cableTier, int area, int energyCapacity, int energyTransfer) {
            this.id = RollMod.id(path);
            this.cableTier = cableTier;
            this.area = area;
            this.energyCapacity = energyCapacity;
            this.energyTransfer = energyTransfer;
        }
    }

    public static final ResourceLocation CROP_MANAGER_MK2 = CropManagerTier.MV.id;

    private static final String HERBICIDE_TRAIT = "herbicide";
    private static final String OUTPUT_TRAIT = "output";
    private static final String ENERGY_TRAIT = "energy";

    private static final int OUTPUT_SLOTS = 18;
    /** How often a Crop Manager scans its area. */
    private static final int HARVEST_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public void onRegisterMachines(MBDRegistryEvent.Machine event) {
        for (CropManagerTier tier : CropManagerTier.values()) {
            event.register(cropManager(tier));
            RollMod.LOGGER.info("[{}] registered MBD2 machine '{}'", RollMod.MODID, tier.id);
        }
    }

    public static boolean isCropManager(ResourceLocation id) {
        for (CropManagerTier tier : CropManagerTier.values()) {
            if (tier.id.equals(id)) return true;
        }
        return false;
    }

    private static MBDMachineDefinition cropManager(CropManagerTier tier) {
        return MBDMachineDefinition.builder()
                .id(tier.id)
                .rootState(rootState(tier))
                .blockProperties(ConfigBlockProperties.builder()
                        // A horizontal facing is what gives "behind the machine" a meaning.
                        .rotationState(RotationState.NON_Y_AXIS)
                        .destroyTime(3.0f)
                        .explosionResistance(9.0f)
                        .build())
                // The factory is re-invoked per definition load, so each call builds fresh traits.
                .machineSettings(() -> cropManagerSettings(tier))
                // Explicitly off: ConfigRecipeLogicSettings defaults enable=true, which would leave
                // RecipeLogic ticking against a machine that has no recipe type at all. The harvester
                // trait does the work from serverTick() instead.
                .recipeLogicSettings(ConfigRecipeLogicSettings.builder().enable(false).build())
                .build();
    }

    /**
     * Idle and active looks, one model each per tier: {@code block/crop_manager_<tier>} and
     * {@code block/crop_manager_<tier>_active}. They only swap the casing's side texture
     * ({@code crop_manager_side_<tier>}) into the shared {@code crop_manager_mk2} / {@code _active} geometry.
     *
     * <p>Both models are authored facing NORTH with a distinct front (north) and back (south) face;
     * MBD2 bakes a rotated copy per facing and caches it ({@code MBDMachineBlock.getModelState} ->
     * {@code ModelFactory.getRotation}), so no blockstate file and no per-facing variants are needed.
     *
     * <p>A child state with no renderer of its own inherits its parent's, so {@code waiting} follows
     * {@code working} into the active model and {@code suspend} stays on the idle one.
     */
    private static MachineState rootState(CropManagerTier tier) {
        @SuppressWarnings("unchecked")
        MachineState.Builder<MachineState> builder =
                (MachineState.Builder<MachineState>) MachineState.baseBuilder();
        return builder
                .modelRenderer(RollMod.id("block/crop_manager_" + tier.name().toLowerCase(java.util.Locale.ROOT)))
                .child("working", working -> working
                        .modelRenderer(RollMod.id("block/crop_manager_" + tier.name().toLowerCase(java.util.Locale.ROOT) + "_active"))
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
    private static ConfigMachineSettings cropManagerSettings(CropManagerTier tier) {
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
        energy.setCapacity(tier.energyCapacity);
        energy.setMaxReceive(tier.energyTransfer);
        // Not 0: MIEnergyStore extends NeoForge's EnergyStorage, whose extractEnergy is clamped by
        // maxExtract — and that is the same call the harvester pays its operating cost with. External
        // drain is stopped by the IO.IN capability gate instead (see MIEnergyStorageWrapper#extract).
        energy.setMaxExtract(tier.energyTransfer);
        energy.setCableTier(tier.cableTier);
        energy.getCapabilityIO().setAllIO(IO.IN);

        CropHarvesterTraitDefinition harvester = new CropHarvesterTraitDefinition();
        harvester.setName("crop_harvester");
        harvester.setHerbicideSlotName(HERBICIDE_TRAIT);
        harvester.setOutputSlotName(OUTPUT_TRAIT);
        harvester.setEnergyTraitName(ENERGY_TRAIT);
        harvester.setAreaWidth(tier.area);
        harvester.setAreaDepth(tier.area);
        // Once a second rather than every tick: a crop advances on random ticks, about once a minute,
        // and scanning the area is mostly block-entity lookups that find nothing new (spark: ~0.8 s of
        // every 500 s on a busy server, nearly all of it the per-tick scan).
        harvester.setTickInterval(HARVEST_INTERVAL_TICKS);

        List<TraitDefinition> traits = List.of(herbicide, output, energy, harvester);

        return ConfigMachineSettings.builder()
                .hasUI(true)
                .uiTemplate(CropManagerUI.template(output, herbicide, energy, harvester))
                .traitDefinitions(traits)
                .build();
    }
}
