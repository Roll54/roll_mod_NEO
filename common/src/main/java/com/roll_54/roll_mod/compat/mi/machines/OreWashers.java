package com.roll_54.roll_mod.compat.mi.machines;

import aztech.modern_industrialization.compat.rei.machines.MachineCategoryParams;
import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.compat.rei.machines.SteamMode;
import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.components.OverclockComponent;
import aztech.modern_industrialization.machines.guicomponents.ProgressBar;
import aztech.modern_industrialization.machines.init.MachineTier;
import aztech.modern_industrialization.machines.models.MachineCasings;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.MachineRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformers;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.ElectricMultipliedCraftingMultiblockBlockEntity;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.SteamMultipliedCraftingMultiblockBlockEntity;

import static aztech.modern_industrialization.machines.multiblocks.HatchTypes.*;

/**
 * The pack's two MI Tweaks batch ore washers ({@code mi_tweaks:ore_washer} and
 * {@code mi_tweaks:steam_ore_washer}, from {@code MITweaksMachineEvents.registerBatchMultiblocks}),
 * rebuilt here under {@code roll_mod:} with the same shapes, recipe types, parallels and EU costs.
 *
 * <p>Both block entities are the ones MI Tweaks' {@code electric} and {@code steamStandalone} build.
 * The electric washer runs {@code modern_industrialization:lv_ore_washer}, the single-block washer's
 * recipes, and joins that recipe type's viewer categories. The steam washer runs its own
 * {@code modern_industrialization:steam_ore_washer} and has a category of its own, listing the recipes
 * at or under 4 EU/t, as MI Tweaks' steam machines do. Both recipe types are created in
 * {@link MachineRegistry.RecipeTypes}, or reused when the pack's KubeJS still registers them.
 *
 * <p>Shapes read as {@link ParallelMultiblocks} describes: back to front, each layer bottom first.
 */
public final class OreWashers {

    public static final String ORE_WASHER = "ore_washer";
    public static final String ORE_WASHER_NAME = "Ore Washer";
    public static final String ORE_WASHER_CASING = "clean_stainless_steel_machine_casing";
    /** The KubeJS washer's overlay folder, which the pack ships under MI's namespace. */
    public static final ResourceLocation ORE_WASHER_OVERLAYS =
            ResourceLocation.parse("modern_industrialization:block/machines/ore_washer");

    public static final String STEAM_ORE_WASHER = "steam_ore_washer";
    public static final String STEAM_ORE_WASHER_NAME = "Steam Ore Washer";
    /** The controller wears the steel plated bricks it is built from: EI's machine casing of that block. */
    public static final String STEAM_ORE_WASHER_CASING = "extended_industrialization:steel_plated_bricks";
    /** The KubeJS washer's overlay folder, which the pack ships under MI Tweaks' namespace. */
    public static final ResourceLocation STEAM_ORE_WASHER_OVERLAYS =
            ResourceLocation.parse("mi_tweaks:block/machines/steam_ore_washer");

    private static final String[][] ORE_WASHER_LAYERS = {
            { "BBBBBBBBB", "CCC   CCC", "CFC   CfC", "CFC   CfC", "CCC   CCC", " C     C ", " C     C " },
            { "BBBBBBBBB", "CPCPPPCPC", "IPCEEECPi", "IPC   CPi", "CPC   CPC", "CPC   CPC", "CCC   CCC" },
            { "BBBBBBBBB", "CCCPPPCCC", "CCC   CCC", "CCC   CCC", "CCC   CCC", " C     C ", " C     C " },
            { "BBBBBBBBB", "  CP#PC  ", "  CCCCC  ", "         ", "         ", "         ", "         " },
    };

    private static final String[][] STEAM_ORE_WASHER_LAYERS = {
            { "IIIII", " iii ", " iii " },
            { "IBBBI", "iS Si", "i   i" },
            { "IBBBI", "iS Si", "i   i" },
            { "IBBBI", "iS Si", "i   i" },
            { "IIIII", " i#i ", " iii " },
    };

    private static boolean oreWasherRegistered;

    private OreWashers() {}

    /** Call from the multiblock hook, after the recipe types are bound. */
    public static void register(MultiblockMachinesMIHookContext hook) {
        registerOreWasher(hook);
        registerSteamOreWasher(hook);
    }

    private static void registerOreWasher(MultiblockMachinesMIHookContext hook) {
        MachineRecipeType recipeType = MachineRegistry.RecipeTypes.ORE_WASHER;
        if (recipeType == null) {
            RollMod.LOGGER.warn("[MI] Skipping multiblock '{}', its recipe type is missing.", ORE_WASHER);
            return;
        }
        SimpleMember casing = member("modern_industrialization:clean_stainless_steel_machine_casing");
        ShapeTemplate shape = new ShapeTemplate.LayeredBuilder(MachineCasings.CLEAN_STAINLESS_STEEL, ORE_WASHER_LAYERS)
                .key('B', member("modern_industrialization:fire_clay_bricks"), HatchFlags.NO_HATCH)
                .key('P', member("modern_industrialization:stainless_steel_machine_casing_pipe"), HatchFlags.NO_HATCH)
                .key('C', casing, HatchFlags.NO_HATCH)
                .key('F', casing, hatches(FLUID_INPUT))
                .key('I', casing, hatches(ITEM_INPUT))
                .key('f', casing, hatches(FLUID_OUTPUT))
                .key('i', casing, hatches(ITEM_OUTPUT))
                .key('E', casing, hatches(ENERGY_INPUT))
                .build();

        hook.builder(ORE_WASHER, ORE_WASHER_NAME,
                        bep -> new ElectricMultipliedCraftingMultiblockBlockEntity(bep, RollMod.id(ORE_WASHER),
                                new ShapeTemplate[] { shape }, MachineTier.MULTIBLOCK, recipeType, 128,
                                EuCostTransformers.percentage(() -> 0.75f)))
                .registerMachine()
                .registerMultiblockShape(shape);
        oreWasherRegistered = true;
    }

    private static void registerSteamOreWasher(MultiblockMachinesMIHookContext hook) {
        MachineRecipeType recipeType = MachineRegistry.RecipeTypes.STEAM_ORE_WASHER;
        if (recipeType == null) {
            RollMod.LOGGER.warn("[MI] Skipping multiblock '{}', its recipe type is missing.", STEAM_ORE_WASHER);
            return;
        }
        // Its hatch casing is EI's; without EI the shape could never form.
        if (!ModList.get().isLoaded("extended_industrialization")) {
            RollMod.LOGGER.warn("[MI] Skipping multiblock '{}', Extended Industrialization is absent.", STEAM_ORE_WASHER);
            return;
        }
        SimpleMember bricks = member("extended_industrialization:steel_plated_bricks");
        ShapeTemplate shape = new ShapeTemplate.LayeredBuilder(MachineCasings.STEEL, STEAM_ORE_WASHER_LAYERS)
                .key('I', bricks, hatches(ITEM_INPUT, ITEM_OUTPUT, FLUID_INPUT))
                .key('i', bricks, HatchFlags.NO_HATCH)
                .key('B', member("modern_industrialization:steel_machine_casing"), HatchFlags.NO_HATCH)
                .key('S', member("modern_industrialization:steel_machine_casing_pipe"), HatchFlags.NO_HATCH)
                .build();

        hook.builder(STEAM_ORE_WASHER, STEAM_ORE_WASHER_NAME,
                        bep -> new SteamMultipliedCraftingMultiblockBlockEntity(bep, RollMod.id(STEAM_ORE_WASHER),
                                new ShapeTemplate[] { shape }, OverclockComponent.getDefaultCatalysts(), recipeType, 32,
                                EuCostTransformers.percentage(() -> 1f)))
                .gui(true, SteamMode.STEAM_ONLY, recipeType, gui -> gui
                        .progressBar(84, 40, "macerate")
                        .slots(slots -> slots.append(
                                in -> in.addSlots(14, 35, 3, 1),
                                out -> out.addSlots(120, 35, 3, 1),
                                fluidIn -> fluidIn.addSlot(14, 53),
                                fluidOut -> {}))
                        .predicate(recipe -> recipe.eu <= 4))
                .registerMachine()
                .registerMultiblockShape(shape)
                .registerRecipeCategory();
    }

    /**
     * Shows the electric washer in every viewer category of {@code lv_ore_washer}, the way the KubeJS
     * version joined the single-block washer's. When the pack no longer has that machine, there is no
     * such category, so it gets one of its own, laid out as the KubeJS multiblock's was. Call from
     * common setup, when every category exists.
     */
    public static void registerWorkstations() {
        if (!oreWasherRegistered) return;
        MachineRecipeType type = MachineRegistry.RecipeTypes.ORE_WASHER;
        ResourceLocation machine = RollMod.id(ORE_WASHER);
        var categories = ReiMachineRecipes.categories.values().stream()
                .filter(params -> params.recipeType == type)
                .map(params -> params.category)
                .toList();
        categories.forEach(category -> ReiMachineRecipes.registerWorkstation(category, machine));
        if (!categories.isEmpty() || ReiMachineRecipes.categories.containsKey(machine)) return;
        ReiMachineRecipes.registerCategory(machine, new MachineCategoryParams(ORE_WASHER_NAME, machine,
                new SlotPositions.Builder().addSlots(56, 35, 1, 2).build(),
                new SlotPositions.Builder().addSlots(102, 35, 2, 2).build(),
                new SlotPositions.Builder().addSlots(36, 35, 1, 2).build(),
                new SlotPositions.Builder().addSlots(140, 35, 1, 2).build(),
                new ProgressBar.Params(77, 33, "triple_arrow"), type, recipe -> true, true,
                SteamMode.ELECTRIC_ONLY));
        ReiMachineRecipes.registerWorkstation(machine, machine);
    }

    private static SimpleMember member(String id) {
        return SimpleMember.forBlockId(ResourceLocation.parse(id));
    }

    private static HatchFlags hatches(aztech.modern_industrialization.machines.multiblocks.HatchType... types) {
        return new HatchFlags.Builder().with(types).build();
    }
}
