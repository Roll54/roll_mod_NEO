package com.roll_54.roll_mod.compat.mi.machines;

import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.compat.rei.machines.MachineCategoryParams;
import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.compat.rei.machines.SteamMode;
import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.guicomponents.ProgressBar;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.blockentities.multiblocks.ElectricBlastFurnaceBlockEntity;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.init.MachineTier;
import aztech.modern_industrialization.machines.models.MachineCasing;
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
import net.swedz.tesseract.neoforge.compat.mi.machine.builder.SpecialMachineBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static aztech.modern_industrialization.machines.multiblocks.HatchTypes.*;

/**
 * Electric multiblocks that run an existing recipe type many times in parallel, registered under
 * {@code roll_mod:} through Tesseract (see {@link com.roll_54.roll_mod.compat.mi.RMMIRegistries}).
 *
 * <p>Each one is Tesseract's {@link ElectricMultipliedCraftingMultiblockBlockEntity} at
 * {@link MachineTier#MULTIBLOCK}, with every extra parallel costing {@link Spec#euPerParallel()} of a
 * recipe's EU — the same block entity MI Tweaks' batch multiblocks build, minus MI Tweaks'
 * {@code mi_tweaks:} id. A spec with a {@link Spec#maxBaseEu()} also refuses recipes above that EU/t,
 * the way an EBF's coil tier does.
 *
 * <h2>Reading the shapes</h2>
 *
 * <p>{@code ShapeTemplate.LayeredBuilder} maps {@code layers[i][j].charAt(k)} to {@code (x = k, y = j,
 * z = -i)} relative to the controller ({@code #}). So the outer array is depth, from the back of the
 * machine to its front (the controller's layer comes last); each inner row is one height level,
 * <b>bottom first</b>; characters run along X as seen from behind the controller. Y and Z are swapped
 * compared to a structure file, and since MI mirrors X for a north-facing controller, a structure
 * saved facing north reads with X reversed. The shapes below were converted from the structure
 * files and checked block-for-block against MI's own placement math.
 *
 * <p>Keys of the tungstensteel shapes ({@link #MEMBERS}): {@code C} tungstensteel heat proof casing (the only
 * spot hatches can go), {@code P} tungstensteel pipe casing, {@code I} incoloy pipe casing, {@code O} osmiridium
 * pipe casing, {@code N} netherite frame, {@code W} tungsten coil, {@code ' '} anything. The later machines bring
 * their own keys, listed on each; in every shape only the {@link Spec#hatchKey()} block takes hatches.
 *
 * <p>A shape whose blocks come from a mod that is not loaded is skipped: MI looks member blocks up lazily, and an
 * unknown id resolves to air, which would let the machine form with that block missing.
 */
public final class ParallelMultiblocks {

    /** Controller and hatch casing; renders as the tungstensteel heat proof casing the shapes are built from. */
    public static final String CASING = "roll_tungstensteel_casing";
    /** Casings of the later machines, under the names MI resolves them by (see {@code MachineRegistry.blockImitationCasings}). */
    public static final String ENDERIUM_CASING = "enderium_casing";
    public static final String INCOLOY_CASING = "turbine_casing";
    public static final String SOLID_TITANIUM_CASING = "solid_titanium_machine_casing";
    public static final String TITANIUM_64_CASING = "roll_titanium_64_casing";
    public static final String CHROME_VANADIUM_STEEL_CASING = "roll_chrome_vanadium_steel_casing";

    /**
     * A machine and everything needed to register and render it.
     *
     * @param euPerParallel share of a recipe's EU each parallel costs (1 = full price)
     * @param maxBaseEu  highest recipe EU/t accepted, like an EBF coil tier's heat; 0 for no limit
     * @param hatchKey   the one key whose blocks can be swapped for hatches
     * @param overlays   texture folder of the controller's front overlay, reused from the machine this
     *                   one is the big version of
     */
    public record Spec(String id, String englishName, Supplier<MachineRecipeType> recipeType, int parallel,
                       float euPerParallel, long maxBaseEu, String casing, char hatchKey, HatchFlags hatches, Map<Character, String> members, String[][] layers,
                       ResourceLocation overlays) {}

    private static final HatchFlags ITEM_FLUID_ENERGY = new HatchFlags.Builder()
            .with(ITEM_INPUT, ITEM_OUTPUT, FLUID_INPUT, FLUID_OUTPUT, ENERGY_INPUT).build();
    private static final HatchFlags ITEM_ENERGY = new HatchFlags.Builder()
            .with(ITEM_INPUT, ITEM_OUTPUT, ENERGY_INPUT).build();

    private static final Map<Character, String> MEMBERS = Map.of(
            'C', "modern_industrialization:tungsten_steel_heat_proof_casing",
            'P', "modern_industrialization:tunstensteel_machine_casing_pipe",
            'I', "modern_industrialization:incoloy_machine_casing_pipe",
            'O', "modern_industrialization:osmiridium_machine_casing_pipe",
            'N', "roll_mod:netherite_frame",
            'W', "modern_industrialization:tungsten_coil");

    public static final Spec HEAT_EXCHANGER = new Spec(
            "highly_advanced_heat_exchanger", "Highly Advanced Heat Exchanger",
            () -> MIMachineRecipeTypes.HEAT_EXCHANGER, 512, 1f, 0, CASING, 'C', ITEM_FLUID_ENERGY, MEMBERS,
            new String[][] {
                    { "CCCCC", "N   N", "N   N", "NNNNN", "N   N", "N   N", "NNNNN", "N   N", "N   N", "CCCCC" },
                    { "CCCCC", " PPI ", " PII ", "NIIPN", " IPP ", " PPO ", "NPOON", " OOP ", " OPP ", "CCCCC" },
                    { "CCCCC", " O I ", " P P ", "NP PN", " I O ", " I O ", "NP PN", " P P ", " O I ", "CCCCC" },
                    { "CCCCC", " OPP ", " OOP ", "NPOON", " PPO ", " IPP ", "NIIPN", " PII ", " PPI ", "CCCCC" },
                    { "CC#CC", "N   N", "N   N", "NNNNN", "N   N", "N   N", "NNNNN", "N   N", "N   N", "CCCCC" },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/heat_exchanger"));

    public static final Spec PYROLYSE_OVEN = new Spec(
            "parallel_pyrolyse_oven", "Parallel Pyrolyse Oven",
            () -> com.roll_54.roll_mod.registry.MachineRegistry.RecipeTypes.PYROLYSE_OVEN, 128, 0.75f, 0, CASING, 'C', ITEM_FLUID_ENERGY, MEMBERS,
            new String[][] {
                    { "CPPPC", "CCCCC", "PCPCP", "CCCCC", "CPPPC" },
                    { " N N ", " WWW ", "PWWWP", " WWW ", " N N " },
                    { "NNNNN", "NWWWN", "PW WP", "NWWWN", "NNNNN" },
                    { " N N ", " WWW ", "PW WP", " WWW ", " N N " },
                    { "NNNNN", "NWWWN", "PW WP", "NWWWN", "NNNNN" },
                    { " N N ", " WWW ", "PW WP", " WWW ", " N N " },
                    { "CPPPC", "CCCCC", "PC#CP", "CCCCC", "CPPPC" },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/pyrolyse_oven"));

    public static final Spec ROTARY_HEARTH_FURNACE = new Spec(
            "rotary_hearth_furnace", "Rotary Hearth Furnace",
            () -> MIMachineRecipeTypes.BLAST_FURNACE, 128, 0.75f, 1024, CASING, 'C', ITEM_FLUID_ENERGY, MEMBERS,
            new String[][] {
                    { " CCCCC ", "  N N  ", "  N N  ", "       ", "       ", "       ", "  N N  ", "  N N  ", " CCCCC " },
                    { "CCCCCCC", " NPNPN ", "       ", "  N N  ", "  PNP  ", "  N N  ", "       ", " NPNPN ", "CCCCCCC" },
                    { "CCPCPCC", "NPPPPPN", "N WWW N", " NWWWN ", " PPPPP ", " NWWWN ", "N WWW N", "NPPPPPN", "CCPCPCC" },
                    { "CCCCCCC", " NPPPN ", "  W W  ", "  W W  ", " NP PN ", "  W W  ", "  W W  ", " NPPPN ", "CCCCCCC" },
                    { "CCPCPCC", "NPPPPPN", "N WWW N", " NWWWN ", " PPPPP ", " NWWWN ", "N WWW N", "NPPPPPN", "CCPCPCC" },
                    { "CCCCCCC", " NPNPN ", "       ", "  N N  ", "  PNP  ", "  N N  ", "       ", " NPNPN ", "CCCCCCC" },
                    { " CC#CC ", "  N N  ", "  N N  ", "       ", "       ", "       ", "  N N  ", "  N N  ", " CCCCC " },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/electric_blast_furnace"));

    public static final Spec LARGE_FURNACE = new Spec(
            "highly_advanced_large_furnace", "Highly Advanced Large Furnace",
            () -> MIMachineRecipeTypes.FURNACE, 128, 0.75f, 0, CASING, 'C', ITEM_ENERGY, MEMBERS,
            new String[][] {
                    { "NCCCN", "N   N", "N   N", "NCCCN" },
                    { "CCPCC", " WWW ", " WWW ", "CCPCC" },
                    { "CPPPC", " W W ", " W W ", "CPPPC" },
                    { "CCPCC", " WWW ", " WWW ", "CCPCC" },
                    { "NC#CN", "N   N", "N   N", "NCCCN" },
            },
            ResourceLocation.parse("extended_industrialization:block/machines/large_furnace"));

    /** 5 wide, 6 tall, 5 deep; controller bottom-front-centre. */
    public static final Spec LARGE_ALLOY_SMELTER = new Spec(
            "large_alloy_smelter", "Large Alloy Smelter",
            () -> com.roll_54.roll_mod.registry.MachineRegistry.RecipeTypes.ALLOY_SMELTER, 128, 0.75f, 0,
            CASING, 'C', ITEM_ENERGY, MEMBERS,
            new String[][] {
                    { "CCCCC", "N   N", "     ", "     ", "NNNNN", "     " },
                    { "CPPPC", "     ", "N W N", "N W N", " WWW ", "  W  " },
                    { "CPPPC", "     ", " W W ", "NWWWN", " W W ", " WWW " },
                    { "CPPPC", "     ", "N W N", "N W N", " WWW ", "  W  " },
                    { "CC#CC", "N   N", "     ", "     ", "NNNNN", "     " },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/alloy_smelter"));

    /** 5 wide, 5 tall, 7 deep. {@code E} enderium casing, {@code T} titanium pipe casing, {@code B} netherite block. */
    public static final Spec HIGHLY_ADVANCED_BENDING_MACHINE = new Spec(
            "highly_advanced_bending_machine", "Highly Advanced Bending Machine",
            () -> MachineRegistry.RecipeTypes.BENDING_MACHINE, 128, 0.75f, 0, ENDERIUM_CASING, 'E', ITEM_FLUID_ENERGY,
            Map.of(
                    'E', "modern_industrialization:enderium_casing",
                    'T', "modern_industrialization:titanium_machine_casing_pipe",
                    'B', "minecraft:netherite_block"),
            new String[][] {
                    { "EEEEE", "EEEEE", "EEEEE", "EEEEE", "EEEEE" },
                    { "EEEEE", "ETETE", "EB BE", "ET TE", "EEEEE" },
                    { "EEEEE", "ETETE", "EB BE", "ET TE", "EEEEE" },
                    { "EEEEE", "ETETE", "EB BE", "ET TE", "EEEEE" },
                    { "EEEEE", "ETETE", "EB BE", "ET TE", "EEEEE" },
                    { "EEEEE", "ETETE", "EB BE", "ET TE", "EEEEE" },
                    { "EEEEE", "EEEEE", "EE#EE", "EEEEE", "EEEEE" },
            },
            ResourceLocation.parse("extended_industrialization:block/machines/bending_machine"));

    /** 3 wide, 4 tall, 8 deep. {@code C} titanium 64 casing, {@code H} highly advanced hull, {@code G} AE2 quartz glass, {@code I} incoloy pipe casing, {@code L} lapotronic laser block. */
    public static final Spec CIRCUIT_ASSEMBLY_LINE = new Spec(
            "circuit_assembly_line", "Circuit Assembly Line",
            () -> MachineRegistry.RecipeTypes.existing("circuit_assembler"), 32, 1f, 0, TITANIUM_64_CASING, 'C', ITEM_FLUID_ENERGY,
            Map.of(
                    'C', "modern_industrialization:titanium_64_machine_casing",
                    'H', "modern_industrialization:highly_advanced_machine_hull",
                    'G', "ae2:quartz_glass",
                    'I', "modern_industrialization:incoloy_machine_casing_pipe",
                    'L', "roll_mod:lapotronic_laser_block"),
            new String[][] {
                    { "CCC", "CCC", "CCC", " C " },
                    { "CHC", "GIG", "CLC", "CCC" },
                    { "CHC", "GIG", "CLC", " C " },
                    { "CHC", "GIG", "CLC", " C " },
                    { "CHC", "GIG", "CLC", " C " },
                    { "CHC", "GIG", "CLC", " C " },
                    { "CHC", "GIG", "CLC", "CCC" },
                    { "CCC", "C#C", "CCC", " C " },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/assembler"));

    /** 4 wide, 3 tall, 5 deep. {@code C} incoloy casing, {@code G} AE2 quartz glass, {@code I} incoloy pipe casing, {@code H} highly advanced hull. */
    public static final Spec ASSEMBLY_LINE = new Spec(
            "assembly_line", "Assembly Line",
            () -> MIMachineRecipeTypes.ASSEMBLER, 32, 1f, 0, INCOLOY_CASING, 'C', ITEM_FLUID_ENERGY,
            Map.of(
                    'C', "modern_industrialization:incoloy_casing",
                    'G', "ae2:quartz_glass",
                    'I', "modern_industrialization:incoloy_machine_casing_pipe",
                    'H', "modern_industrialization:highly_advanced_machine_hull"),
            new String[][] {
                    { "CCC ", "CCC ", "CCC " },
                    { "CCCC", "GIHC", "CCCC" },
                    { "CCCC", "GIHC", "CCCC" },
                    { "CCCC", "GIHC", "CCCC" },
                    { "CCCC", "C#GC", "CCCC" },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/assembler"));

    /** 5 wide, 5 tall, 5 deep. {@code E} enderium casing, {@code K} kanthal coil, {@code S} solid titanium casing, {@code T} titanium pipe casing. */
    public static final Spec NETHERSTORM_THERMAL_CENTRIFUGE = new Spec(
            "netherstorm_thermal_centrifuge", "Netherstorm Thermal Centrifuge",
            () -> MachineRegistry.RecipeTypes.existing("thermal_centrefuge"), 128, 0.75f, 0, ENDERIUM_CASING, 'E', ITEM_FLUID_ENERGY,
            Map.of(
                    'T', "modern_industrialization:titanium_machine_casing_pipe",
                    'S', "modern_industrialization:solid_titanium_machine_casing",
                    'E', "modern_industrialization:enderium_casing",
                    'K', "modern_industrialization:kanthal_coil"),
            new String[][] {
                    { " T T ", " EEE ", "EE EE", " EEE ", " T T " },
                    { "TTSTT", "EKKKE", "E E E", "EKKKE", "TTSTT" },
                    { " SSS ", "EKKKE", " E E ", "EKKKE", " SSS " },
                    { "TTSTT", "EKKKE", "E # E", "EKKKE", "TTSTT" },
                    { " T T ", " EEE ", "EE EE", " EEE ", " T T " },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/centrifuge"));

    /** 9 wide, 11 tall, 16 deep. {@code C} chrome vanadium steel block, {@code B} netherite block, {@code S} nether star block (Extended Crafting), {@code W} tungsten block. */
    public static final Spec HEPHAESTUS_COMPRESSOR = new Spec(
            "hephaestus_compressor", "Hephaestus Compressor",
            () -> MIMachineRecipeTypes.COMPRESSOR, 512, 0.75f, 0, CHROME_VANADIUM_STEEL_CASING, 'C', ITEM_FLUID_ENERGY,
            Map.of(
                    'C', "modern_industrialization:chrome_vanadium_steel_block",
                    'B', "minecraft:netherite_block",
                    'S', "extendedcrafting:nether_star_block",
                    'W', "modern_industrialization:tungsten_block"),
            new String[][] {
                    { "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  " },
                    { "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  " },
                    { "CCCCCCCCC", "CCCCCCCCC", "         ", "         ", "         ", "         ", "         ", "         ", "  CC CC  ", "  CC CC  ", "  CC CC  " },
                    { "CCCCCCCCC", "CCCCCCCCC", "         ", "         ", "         ", "         ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCBCC  " },
                    { " CCCCCCC ", " CCCCCCC ", "  CCCCC  ", "  CCCCC  ", "         ", "         ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CSWSC  " },
                    { " CCCCCCC ", " CCCCCCC ", "  CCCCC  ", "  CCCCC  ", "   CCC   ", "   CCC   ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CSBSC  " },
                    { " CCCCCCC ", " CCCCCCC ", "  CCCCC  ", "  CCCCC  ", "   CCC   ", "   CCC   ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CBBBC  ", "  CWBWC  " },
                    { " CCCCCCC ", " CCCCCCC ", "  CCCCC  ", "  CCCCC  ", "   CCC   ", "   CCC   ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CSBSC  " },
                    { " CCCCCCC ", " CCCCCCC ", "  CCCCC  ", "  CCCCC  ", "   CCC   ", "   C#C   ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CSWSC  " },
                    { " CCCCCCC ", " CCCCCCC ", "  CCCCC  ", "  CCCCC  ", "         ", "         ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCBCC  " },
                    { "CCCCCCCCC", "CCCCCCCCC", "         ", "         ", "         ", "         ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  ", "  CCCCC  " },
                    { "CCCCCCCCC", "CCCCCCCCC", "         ", "         ", "         ", "         ", "         ", "         ", "   CCC   ", "   CCC   ", "   CCC   " },
                    { "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "   CCC   ", "   CCC   ", "   CCC   " },
                    { "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "    C    ", "    C    " },
                    { "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "    C    ", "    C    " },
                    { "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "    C    " },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/compressor"));

    /** 5 wide, 5 tall, 5 deep. {@code E} enderium casing, {@code F} PTFE pipe casing. */
    public static final Spec LARGE_MIXER = new Spec(
            "large_mixer", "Large Mixer",
            () -> MIMachineRecipeTypes.MIXER, 128, 0.75f, 0, ENDERIUM_CASING, 'E', ITEM_FLUID_ENERGY,
            Map.of(
                    'E', "modern_industrialization:enderium_casing",
                    'F', "modern_industrialization:polytetrafluoroethylene_machine_casing_pipe"),
            new String[][] {
                    { " EEE ", "EEEEE", " EEE ", " EEE ", "EEEEE" },
                    { "EEEEE", "E F E", "E   E", "E F E", "E   E" },
                    { "EEEEE", "EFFFE", "E F E", "EFFFE", "E   E" },
                    { "EEEEE", "E F E", "E   E", "E F E", "E   E" },
                    { " EEE ", "EE#EE", " EEE ", " EEE ", "EEEEE" },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/mixer"));

    /** 5 wide, 5 tall, 5 deep. {@code C} titanium 64 casing, {@code G} AE2 quartz glass, {@code I} incoloy pipe casing. */
    public static final Spec PERMAFROST_VACUUM_FREEZER = new Spec(
            "permafrost_vacuum_freezer", "Permafrost Vacuum Freezer",
            () -> MIMachineRecipeTypes.VACUUM_FREEZER, 32, 1f, 0, TITANIUM_64_CASING, 'C', ITEM_FLUID_ENERGY,
            Map.of(
                    'C', "modern_industrialization:titanium_64_machine_casing",
                    'G', "ae2:quartz_glass",
                    'I', "modern_industrialization:incoloy_machine_casing_pipe"),
            new String[][] {
                    { "CCCCC", "CGGGC", "CGGGC", "CGGGC", "CCCCC" },
                    { "CGGGC", "GCICG", "GCCCG", "GCICG", "CGGGC" },
                    { "CGGGC", "GIIIG", "GCICG", "GIIIG", "CGGGC" },
                    { "CGGGC", "GCICG", "GCCCG", "GCICG", "CGGGC" },
                    { "CC#CC", "CGGGC", "CGGGC", "CGGGC", "CCCCC" },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/vacuum_freezer"));

    /** 5 wide, 6 tall, 5 deep. {@code E} enderium casing, {@code B} netherite block. */
    public static final Spec LARGE_COMPRESSOR = new Spec(
            "large_compressor", "Large Compressor",
            () -> MIMachineRecipeTypes.COMPRESSOR, 64, 1f, 0, ENDERIUM_CASING, 'E', ITEM_FLUID_ENERGY,
            Map.of(
                    'E', "modern_industrialization:enderium_casing",
                    'B', "minecraft:netherite_block"),
            new String[][] {
                    { "EEEEE", "EEEEE", " EEE ", "EEEEE", " EEE ", "EEEEE" },
                    { "EEEEE", "EBBBE", "E   E", "E   E", "EBBBE", "EEEEE" },
                    { "EEEEE", "EBBBE", "E   E", "E   E", "EBBBE", "EEEEE" },
                    { "EEEEE", "EBBBE", "E   E", "E   E", "EBBBE", "EEEEE" },
                    { "EE#EE", "EEEEE", " EEE ", "EEEEE", " EEE ", "EEEEE" },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/compressor"));

    /** 7 wide, 3 tall, 3 deep. {@code S} solid titanium casing, {@code T} titanium pipe casing. */
    public static final Spec LARGE_CUTTING_MACHINE = new Spec(
            "large_cutting_machine", "Large Cutting Machine",
            () -> MIMachineRecipeTypes.CUTTING_MACHINE, 128, 0.75f, 0, SOLID_TITANIUM_CASING, 'S', ITEM_FLUID_ENERGY,
            Map.of(
                    'S', "modern_industrialization:solid_titanium_machine_casing",
                    'T', "modern_industrialization:titanium_machine_casing_pipe"),
            new String[][] {
                    { "STTTSSS", "SSTSS  ", "  S    " },
                    { "SSSSSSS", "S      ", "SSSSS  " },
                    { "SSSSSSS", "SS#SS  ", "  S    " },
            },
            ResourceLocation.parse("modern_industrialization:block/machines/cutting_machine"));

    /** Everything this class registers, in registration order. Datagen reads models from it. */
    public static final List<Spec> ALL = List.of(
            HEAT_EXCHANGER, PYROLYSE_OVEN, ROTARY_HEARTH_FURNACE, LARGE_FURNACE, LARGE_ALLOY_SMELTER,
            HIGHLY_ADVANCED_BENDING_MACHINE, CIRCUIT_ASSEMBLY_LINE, ASSEMBLY_LINE, NETHERSTORM_THERMAL_CENTRIFUGE,
            HEPHAESTUS_COMPRESSOR, LARGE_MIXER, PERMAFROST_VACUUM_FREEZER, LARGE_COMPRESSOR, LARGE_CUTTING_MACHINE);

    private ParallelMultiblocks() {}

    /** Call from the multiblock hook, after {@link #CASING} has been registered. */
    public static void register(MultiblockMachinesMIHookContext hook) {
        for (Spec spec : ALL) {
            MachineRecipeType recipeType = spec.recipeType().get();
            if (recipeType == null) {
                RollMod.LOGGER.warn("[MI] Skipping multiblock '{}', its recipe type is missing.", spec.id());
                continue;
            }
            String missingMod = missingMod(spec);
            if (missingMod != null) {
                RollMod.LOGGER.warn("[MI] Skipping multiblock '{}', its blocks need '{}', which is not loaded.",
                        spec.id(), missingMod);
                continue;
            }
            ShapeTemplate shape = shape(spec, MachineCasings.get(spec.casing()));

            SpecialMachineBuilder builder = hook.builder(spec.id(), spec.englishName(),
                    bep -> new BlockEntity(bep, spec, recipeType, shape));
            builder.registerMachine();
            builder.registerMultiblockShape(shape);
            REGISTERED.add(spec);
        }
    }

    private static final List<Spec> REGISTERED = new ArrayList<>();

    /**
     * Shows each registered controller as a workstation in every recipe-viewer category of its recipe
     * type, and gives a heat-limited machine a category of its own. Call from common setup: by then
     * every category exists, the KubeJS- and EI-registered ones included, and the recipe viewers only
     * read them once recipes load.
     */
    public static void registerWorkstations() {
        for (Spec spec : REGISTERED) {
            MachineRecipeType type = spec.recipeType().get();
            if (type == null) {
                RollMod.LOGGER.warn("[MI] No recipe type for '{}', so no recipe-viewer categories.", spec.id());
                continue;
            }
            ResourceLocation machine = RollMod.id(spec.id());
            List<ResourceLocation> categories = ReiMachineRecipes.categories.values().stream()
                    .filter(params -> params.recipeType == type)
                    .map(params -> params.category)
                    .filter(category -> !aboveHeat(category, spec.maxBaseEu()))
                    .toList();
            for (ResourceLocation category : categories) {
                ReiMachineRecipes.registerWorkstation(category, machine);
            }
            RollMod.LOGGER.debug("[MI] '{}' is a workstation for {}", spec.id(), categories);
            // Own category when nothing lists this type's recipes (the pyrolyse oven's used to come from
            // its KubeJS machine), or when a heat limit cuts across the existing ones.
            if (categories.isEmpty() || spec.maxBaseEu() > 0) {
                registerOwnCategory(spec, type, machine);
            }
        }
    }

    /**
     * An EBF coil-tier category lists the recipes above the previous tier's EU/t, up to its own (see
     * MI's {@code MultiblockMachines}). One starting at or above {@code maxBaseEu} has no recipe this
     * machine can run. A category straddling the limit is kept.
     */
    private static boolean aboveHeat(ResourceLocation category, long maxBaseEu) {
        if (maxBaseEu <= 0) return false;
        List<ElectricBlastFurnaceBlockEntity.Tier> tiers = ElectricBlastFurnaceBlockEntity.tiers;
        for (int i = 0; i < tiers.size(); i++) {
            if (category.equals(MI.id("electric_blast_furnace_" + tiers.get(i).coilBlockId().getPath()))) {
                long previousMax = i == 0 ? 4 : tiers.get(i - 1).maxBaseEu();
                return previousMax >= maxBaseEu;
            }
        }
        return false;
    }

    /**
     * A category for the controller alone, listing every recipe of its type it can run (up to its heat,
     * if it has one). Laid out with two slots of each kind, as the pack's KubeJS pyrolyse oven was —
     * enough for both it and the blast furnace. EBF tiers only go as high as the pack's coils, so blast
     * furnace recipes between the top coil and this machine's heat would otherwise show in no category
     * at all. Titled by the lang key {@code rei_categories.roll_mod.<id>}.
     */
    private static void registerOwnCategory(Spec spec, MachineRecipeType type, ResourceLocation machine) {
        if (ReiMachineRecipes.categories.containsKey(machine)) return;
        long maxBaseEu = spec.maxBaseEu();
        ReiMachineRecipes.registerCategory(machine, new MachineCategoryParams(spec.englishName(), machine,
                new SlotPositions.Builder().addSlots(56, 35, 1, 2).build(),
                new SlotPositions.Builder().addSlots(102, 35, 1, 2).build(),
                new SlotPositions.Builder().addSlots(36, 35, 1, 2).build(),
                new SlotPositions.Builder().addSlots(122, 35, 1, 2).build(),
                new ProgressBar.Params(77, 33, "arrow"), type, recipe -> maxBaseEu <= 0 || recipe.eu <= maxBaseEu, true,
                SteamMode.ELECTRIC_ONLY));
        ReiMachineRecipes.registerWorkstation(machine, machine);
    }

    /** The first mod a used member block belongs to that is not loaded, or null. */
    private static String missingMod(Spec spec) {
        String used = String.join("", Arrays.stream(spec.layers()).flatMap(Arrays::stream).toList());
        for (Map.Entry<Character, String> member : spec.members().entrySet()) {
            if (used.indexOf(member.getKey()) < 0) continue;
            String namespace = ResourceLocation.parse(member.getValue()).getNamespace();
            if (!namespace.equals("minecraft") && !ModList.get().isLoaded(namespace)) return namespace;
        }
        return null;
    }

    private static ShapeTemplate shape(Spec spec, MachineCasing casing) {
        ShapeTemplate.LayeredBuilder builder = new ShapeTemplate.LayeredBuilder(casing, spec.layers());
        // MI rejects a key the layers do not use, and MEMBERS is shared between shapes.
        String used = String.join("", Arrays.stream(spec.layers()).flatMap(Arrays::stream).toList());
        spec.members().forEach((key, block) -> {
            if (used.indexOf(key) < 0) return;
            builder.key(key,
                    SimpleMember.forBlockId(ResourceLocation.parse(block)),
                    key == spec.hatchKey() ? spec.hatches() : HatchFlags.NO_HATCH);
        });
        return builder.build();
    }

    /**
     * {@link ElectricMultipliedCraftingMultiblockBlockEntity} built from a {@link Spec}, which also
     * applies the spec's heat limit. The recipe EU checked is the recipe's own, before parallels, as
     * the EBF checks it, and upgrades do not raise it.
     */
    private static final class BlockEntity extends ElectricMultipliedCraftingMultiblockBlockEntity {
        private final long maxBaseEu;

        BlockEntity(BEP bep, Spec spec, MachineRecipeType recipeType, ShapeTemplate shape) {
            super(bep, RollMod.id(spec.id()), new ShapeTemplate[] { shape }, MachineTier.MULTIBLOCK,
                    recipeType, spec.parallel(), EuCostTransformers.percentage(spec::euPerParallel));
            this.maxBaseEu = spec.maxBaseEu();
        }

        @Override
        public boolean isRecipeBanned(long recipeEu) {
            return (maxBaseEu > 0 && recipeEu > maxBaseEu) || super.isRecipeBanned(recipeEu);
        }
    }
}
