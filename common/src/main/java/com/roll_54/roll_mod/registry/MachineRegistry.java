package com.roll_54.roll_mod.registry;

import aztech.modern_industrialization.machines.models.MachineCasing;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;

import com.google.common.collect.Maps;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.mi.EIRecipeTypes;
import com.roll_54.roll_mod.compat.mi.MICasings;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.swedz.mi_tweaks.compat.kubejs.machine.RegisterBatchMultiblocksEventJS;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineCasingsMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineRecipeTypesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;

import java.util.Map;
import java.util.function.Function;

import static com.roll_54.roll_mod.RollMod.MODID;

public class MachineRegistry {


    public static void casings(MachineCasingsMIHookContext hook) {

        Casings.THERMAL_VIBRATION_SAVE_CASING = hook.registerCubeAll("thermal_vibration_save_casing", "Test Bukvi", ResourceLocation.fromNamespaceAndPath(MODID, "block/casings/thermal_vibration_save_casing") );

        // Normally already done from RMMIHookListener's static initializer; harmless if so, since
        // blockImitationCasings is idempotent. Kept as a fallback for the case where this hook is
        // reached without the class having been initialized early.
        blockImitationCasings();
    }

    /**
     * Casings that only imitate an existing block. These live in MI's namespace, so multiblock
     * shapes keep referring to them by bare name -- see {@link MICasings}.
     *
     * <h2>Why this is not just part of {@link #casings}</h2>
     *
     * <p>MI fires its KubeJS {@code MIMachineEvents.registerCasings} event from the static
     * initializer of {@code MachineCasings}, so it runs exactly once, whenever that class is first
     * touched. In a pack where something initializes it before KubeJS has loaded its startup
     * scripts, the event reaches zero handlers and every casing a script registers is lost --
     * {@code MachineCasings.get} then throws "casing model ... does not exist" while MI is
     * registering machines.
     *
     * <p>{@link MICasings#blockImitation} writes into {@code MachineCasings.registeredCasings}
     * directly, so it does not depend on that event firing at a useful time. It does have to run
     * before the first {@code get} for these names, which happens inside MI's own constructor --
     * earlier than any Tesseract MI hook, those being triggered from MI Tweaks' constructor. Hence
     * the call from a static initializer on an {@code @MIHookEntrypoint} class, which Tesseract
     * loads with {@code Class.forName} well before MI is constructed.
     *
     * <p>Safe to call more than once: {@link MICasings#blockImitation} returns any casing already
     * registered under the name rather than failing.
     */
    public static void blockImitationCasings() {
        Casings.SPACE_RESEARCH_STATION_CONTROLLER = MICasings.blockImitation(
                "space_research_station_controller_casing", "modern_industrialization:titanium_64_machine_casing");
        Casings.SPACE_CASING_T2 = MICasings.blockImitation(
                "space_casing_t2", "modern_industrialization:space_casing_t2");
        Casings.INERT_CASING = MICasings.blockImitation(
                "inert_casing", "modern_industrialization:chemically_inert_ptfe_casing");
        Casings.TURBINE_CASING = MICasings.blockImitation(
                "turbine_casing", "modern_industrialization:incoloy_casing");
        Casings.ENDERIUM_CASING = MICasings.blockImitation(
                "enderium_casing", "modern_industrialization:enderium_casing");
    }





    /**
     * Resolves the recipe types this mod borrows from other mods.
     *
     * <p>Call from {@code MIHookListener#machineRecipeTypes}. Tesseract runs its hooks
     * listener-by-listener rather than phase-by-phase -- every phase of one mod's listener before
     * the next mod's -- and orders the listeners by mod load order, so this only sees Extended
     * Industrialization's types because {@code neoforge.mods.toml} declares
     * {@code ordering = "AFTER"} on {@code extended_industrialization}. Without that, roll_mod's
     * turn comes first and every lookup here would come back null.
     */
    public static void recipeTypes(MachineRecipeTypesMIHookContext hook) {
        RecipeTypes.resolveExternal();
    }

    /**
     * Multiblock machines that run on another mod's recipe types.
     *
     * <p>Ported out of the pack's KubeJS startup script: MI Tweaks fires its
     * {@code registerBatchMultiblocks} event from its own listener's multiblock phase, which -- see
     * {@link #recipeTypes} -- lands before Extended Industrialization has registered anything, so a
     * script asking for {@code extended_industrialization:alloy_smelter} there always fails. Doing
     * it from this mod's listener instead puts it after EI.
     *
     * <p>This drives MI Tweaks' own event object directly rather than reimplementing its builder,
     * so the machine is put together exactly as the script did.
     */
    public static void multiblocks(MultiblockMachinesMIHookContext hook) {
        if (RecipeTypes.ALLOY_SMELTER == null) {
            RollMod.LOGGER.warn("[MI] Skipping Large Alloy Smelter, its recipe type is missing.");
            return;
        }

        RegisterBatchMultiblocksEventJS event = new RegisterBatchMultiblocksEventJS(hook);

        HatchFlags hatch = event.hatchOf("item_input", "item_output", "energy_input");
        SimpleMember heatproofCasing = event.memberOfBlock("modern_industrialization:heatproof_machine_casing");
        SimpleMember tungstenCoil = event.memberOfBlock("modern_industrialization:tungsten_coil");

        // 5w x 3h x 3d, controller bottom-front-centre.
        ShapeTemplate shape = event.layeredShape("heatproof_machine_casing", new String[][] {
                        { "HHHHH", "CCMCC", "HHHHH" },
                        { "HHHHH", "CCMCC", "HHHHH" },
                        { "HH#HH", "CCMCC", "HHHHH" },
                })
                .key('H', heatproofCasing, hatch)
                .key('M', heatproofCasing, event.noHatch())
                .key('C', tungstenCoil, event.noHatch())
                .build();

        event.electric(
                "Large Alloy Smelter", "large_alloy_smelter",
                RecipeTypes.ALLOY_SMELTER, shape,
                (workstations) -> {},
                "heatproof_machine_casing", "alloy_smelter", true, false, false,
                128, 0.75f,
                true);
    }

    //helpers
    public static final class Casings {
        public static MachineCasing THERMAL_VIBRATION_SAVE_CASING;

        public static MachineCasing SPACE_RESEARCH_STATION_CONTROLLER;
        public static MachineCasing SPACE_CASING_T2;
        public static MachineCasing INERT_CASING;
        public static MachineCasing TURBINE_CASING;
        public static MachineCasing ENDERIUM_CASING;
    }

    public static final class RecipeTypes {
        public static MachineRecipeType BENDING_MACHINE;
        public static MachineRecipeType ALLOY_SMELTER;
        public static MachineRecipeType CANNING_MACHINE;
        public static MachineRecipeType COMPOSTER;
        public static MachineRecipeType BREWERY;
        private static final Map<MachineRecipeType, String> RECIPE_TYPE_NAMES = Maps.newHashMap();

        public static Map<MachineRecipeType, String> getNames() {
            return RECIPE_TYPE_NAMES;
        }

        /**
         * Binds the recipe types owned by Extended Industrialization. They stay null when EI is
         * absent -- it is an optional dependency -- so anything using them has to null check.
         */
        static void resolveExternal() {
            if (!ModList.get().isLoaded("extended_industrialization")) {
                RollMod.LOGGER.info("[MI] Extended Industrialization is absent, leaving its recipe types unbound.");
                return;
            }
            EIRecipeTypes.bind();
        }

        private static MachineRecipeType create(MachineRecipeTypesMIHookContext hook, String englishName, String id, Function<ResourceLocation, MachineRecipeType> creator) {
            MachineRecipeType recipeType = hook.create(id, creator);
            RECIPE_TYPE_NAMES.put(recipeType, englishName);
            return recipeType;
        }

        private static MachineRecipeType create(MachineRecipeTypesMIHookContext hook, String englishName, String id) {
            return create(hook, englishName, id, MachineRecipeType::new);
        }
    }
}
