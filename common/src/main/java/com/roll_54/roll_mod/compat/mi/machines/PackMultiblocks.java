package com.roll_54.roll_mod.compat.mi.machines;

import aztech.modern_industrialization.compat.rei.machines.SteamMode;
import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.blockentities.multiblocks.ElectricCraftingMultiblockBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.GeneratorMultiblockBlockEntity;
import aztech.modern_industrialization.machines.components.FluidItemConsumerComponent;
import aztech.modern_industrialization.machines.models.MachineCasings;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.MachineRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static aztech.modern_industrialization.machines.multiblocks.HatchTypes.*;

/**
 * The multiblocks the pack's KubeJS startup script used to register through MI's
 * {@code MIMachineEvents.registerMachines} ({@code simpleElectricCraftingMultiBlock} and
 * {@code simpleGeneratorMultiBlock}), rebuilt under {@code roll_mod:} with the same shapes, casings,
 * recipe types and recipe-viewer layouts. The block entities are the ones MI's KubeJS builders make.
 *
 * <p>Their recipe types keep their {@code modern_industrialization:} ids (see
 * {@link MachineRegistry.RecipeTypes}), so the pack's recipes keep working. The front overlays are the
 * KubeJS machines' own, which the pack ships under {@code modern_industrialization:block/machines/}.
 *
 * <p>Shapes read as {@link ParallelMultiblocks} describes: back to front, each layer bottom first; they
 * are the KubeJS arrays as they were.
 */
public final class PackMultiblocks {

    private static final HatchFlags ITEM_FLUID_ENERGY = hatches(ITEM_INPUT, ITEM_OUTPUT, FLUID_INPUT, FLUID_OUTPUT, ENERGY_INPUT);

    /**
     * A crafting multiblock and its recipe-viewer layout.
     *
     * @param hatchKeys keys whose blocks take {@code hatches}; every other key takes none
     * @param slots     item inputs, item outputs, fluid inputs, fluid outputs, as the KubeJS script laid them out
     */
    public record Crafting(String id, String englishName, Supplier<MachineRecipeType> recipeType, String casing,
                           ResourceLocation overlays, Map<Character, String> members, String hatchKeys,
                           HatchFlags hatches, String[][] layers, int progressX, int progressY, String progressBar,
                           Slots slots) {}

    public record Slots(Consumer<SlotPositions.Builder> itemInputs, Consumer<SlotPositions.Builder> itemOutputs,
                        Consumer<SlotPositions.Builder> fluidInputs, Consumer<SlotPositions.Builder> fluidOutputs) {}

    /** 3 wide, 3 tall, 4 deep: heatproof casing around two cupronickel coil rings. */
    public static final Crafting PYROLYSE_OVEN = new Crafting(
            "pyrolyse_oven", "Pyrolyse Oven",
            () -> MachineRegistry.RecipeTypes.PYROLYSE_OVEN, "heatproof_machine_casing",
            ResourceLocation.parse("modern_industrialization:block/machines/pyrolyse_oven"),
            Map.of(
                    'H', "modern_industrialization:heatproof_machine_casing",
                    'C', "modern_industrialization:cupronickel_coil"),
            "H", ITEM_FLUID_ENERGY,
            new String[][] {
                    { "HHH", "HHH", "HHH" },
                    { "CCC", "C C", "CCC" },
                    { "CCC", "C C", "CCC" },
                    { "HHH", "H#H", "HHH" },
            },
            77, 33, "triple_arrow",
            new Slots(in -> in.addSlots(56, 35, 1, 2), out -> out.addSlots(102, 35, 1, 2),
                    fluidIn -> fluidIn.addSlots(36, 35, 1, 2), fluidOut -> fluidOut.addSlots(122, 35, 1, 2)));

    /** 3 wide, 3 tall, 7 deep, two lapotronic lasers inside. */
    public static final Crafting PHOTOLITHOGRAPHER = new Crafting(
            "photolithographer", "Photolithographer",
            () -> MachineRegistry.RecipeTypes.PHOTOLITHOGRAPHER, "solid_titanium_machine_casing",
            ResourceLocation.parse("modern_industrialization:block/machines/assembler"),
            Map.of(
                    'T', "modern_industrialization:solid_titanium_machine_casing",
                    'P', "modern_industrialization:titanium_machine_casing_pipe",
                    'G', "ae2:quartz_vibrant_glass",
                    'A', "roll_mod:lapotronic_laser_block"),
            "T", ITEM_FLUID_ENERGY,
            new String[][] {
                    { "TTT", "TTT", "TTT" },
                    { "TPT", "TPT", "TPT" },
                    { "TPT", "PAP", "TPT" },
                    { "TPT", "GAG", "TPT" },
                    { "TPT", "PAP", "TPT" },
                    { "TPT", "TPT", "TPT" },
                    { "TTT", "T#T", "TTT" },
            },
            78, 23, "wiremill",
            new Slots(in -> in.addSlots(62, 5, 3, 1).addSlots(20, 10, 2, 3), out -> out.addSlot(120, 23),
                    fluidIn -> fluidIn.addSlots(0, 10, 1, 3), fluidOut -> {}));

    public static final List<Crafting> CRAFTING = List.of(PYROLYSE_OVEN, PHOTOLITHOGRAPHER);

    /** The steam turbine: 3 wide, 3 tall, 4 deep of incoloy, up to 65536 EU/t. */
    public static final String TURBINE = "high_efficiency_steam_turbine";
    public static final String TURBINE_NAME = "High Efficiency Steam Turbine";
    public static final String TURBINE_CASING = "turbine_casing";
    public static final ResourceLocation TURBINE_OVERLAYS =
            ResourceLocation.parse("modern_industrialization:block/machines/steam_turbine");
    private static final long TURBINE_MAX_EU = 65536;
    private static final String[][] TURBINE_LAYERS = {
            { "HHH", "HHH", "HHH" },
            { "CCC", "CCC", "CCC" },
            { "CCC", "CCC", "CCC" },
            { "HHH", "H#H", "HHH" },
    };

    private PackMultiblocks() {}

    /** Call from the multiblock hook, after the recipe types are bound. */
    public static void register(MultiblockMachinesMIHookContext hook) {
        for (Crafting machine : CRAFTING) {
            registerCrafting(hook, machine);
        }
        registerTurbine(hook);
    }

    private static void registerCrafting(MultiblockMachinesMIHookContext hook, Crafting machine) {
        MachineRecipeType recipeType = machine.recipeType().get();
        if (recipeType == null) {
            RollMod.LOGGER.warn("[MI] Skipping multiblock '{}', its recipe type is missing.", machine.id());
            return;
        }
        String missing = missingMod(machine.members(), machine.layers());
        if (missing != null) {
            RollMod.LOGGER.warn("[MI] Skipping multiblock '{}', its blocks need '{}', which is not loaded.", machine.id(), missing);
            return;
        }
        ShapeTemplate shape = shape(machine.casing(), machine.layers(), machine.members(), machine.hatchKeys(), machine.hatches());
        ResourceLocation id = RollMod.id(machine.id());
        hook.builder(machine.id(), machine.englishName(),
                        bep -> new ElectricCraftingMultiblockBlockEntity(bep, id, shape, recipeType))
                .gui(true, SteamMode.ELECTRIC_ONLY, recipeType, gui -> gui
                        .progressBar(machine.progressX(), machine.progressY(), machine.progressBar())
                        .slots(slots -> slots.append(machine.slots().itemInputs(), machine.slots().itemOutputs(),
                                machine.slots().fluidInputs(), machine.slots().fluidOutputs())))
                .registerMachine()
                .registerMultiblockShape(shape)
                .registerRecipeCategory();
    }

    private static void registerTurbine(MultiblockMachinesMIHookContext hook) {
        Map<Character, String> members = Map.of(
                'H', "modern_industrialization:incoloy_casing",
                'C', "modern_industrialization:incoloy_machine_casing_pipe");
        ShapeTemplate shape = shape(TURBINE_CASING, TURBINE_LAYERS, members, "H",
                hatches(ITEM_INPUT, ITEM_OUTPUT, FLUID_INPUT, FLUID_OUTPUT, ENERGY_INPUT, ENERGY_OUTPUT));
        // EU per mB of each steam.
        FluidItemConsumerComponent.EuProductionMapBuilder<net.minecraft.world.level.material.Fluid> fluids =
                new FluidItemConsumerComponent.EuProductionMapBuilder<>(BuiltInRegistries.FLUID)
                        .add(ResourceLocation.parse("modern_industrialization:steam"), 1)
                        .add(ResourceLocation.parse("modern_industrialization:heavy_water_steam"), 1)
                        .add(ResourceLocation.parse("modern_industrialization:high_pressure_steam"), 8)
                        .add(ResourceLocation.parse("modern_industrialization:high_pressure_heavy_water_steam"), 8);
        var itemFuels = new FluidItemConsumerComponent.EuProductionMapBuilder<>(BuiltInRegistries.ITEM).build();
        var fluidFuels = fluids.build();
        // MI's generator multiblock takes its id as a name under MI's namespace (its screen title only).
        hook.builder(TURBINE, TURBINE_NAME,
                        bep -> new GeneratorMultiblockBlockEntity(bep, TURBINE, shape,
                                new FluidItemConsumerComponent(TURBINE_MAX_EU, 1.0, itemFuels, fluidFuels, false)))
                .registerMachine()
                .registerMultiblockShape(shape);
    }

    private static ShapeTemplate shape(String casing, String[][] layers, Map<Character, String> members,
                                       String hatchKeys, HatchFlags hatches) {
        ShapeTemplate.LayeredBuilder builder = new ShapeTemplate.LayeredBuilder(MachineCasings.get(casing), layers);
        String used = String.join("", Arrays.stream(layers).flatMap(Arrays::stream).toList());
        members.forEach((key, block) -> {
            if (used.indexOf(key) < 0) return;
            builder.key(key, SimpleMember.forBlockId(ResourceLocation.parse(block)),
                    hatchKeys.indexOf(key) >= 0 ? hatches : HatchFlags.NO_HATCH);
        });
        return builder.build();
    }

    /** The first mod a used member block belongs to that is not loaded, or null. */
    private static String missingMod(Map<Character, String> members, String[][] layers) {
        String used = String.join("", Arrays.stream(layers).flatMap(Arrays::stream).toList());
        for (Map.Entry<Character, String> member : members.entrySet()) {
            if (used.indexOf(member.getKey()) < 0) continue;
            String namespace = ResourceLocation.parse(member.getValue()).getNamespace();
            if (!namespace.equals("minecraft") && !ModList.get().isLoaded(namespace)) return namespace;
        }
        return null;
    }

    private static HatchFlags hatches(HatchType... types) {
        return new HatchFlags.Builder().with(types).build();
    }
}
