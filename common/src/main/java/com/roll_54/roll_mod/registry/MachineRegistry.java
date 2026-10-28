package com.roll_54.roll_mod.registry;

import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.models.MachineCasing;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;

import com.google.common.collect.Maps;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.mi.EIRecipeTypes;
import com.roll_54.roll_mod.compat.mi.MICasings;
import com.roll_54.roll_mod.compat.mi.machines.CoilPyrolyseOven;
import com.roll_54.roll_mod.compat.mi.machines.HighlyAdvancedDistillery;
import com.roll_54.roll_mod.compat.mi.machines.FluidHatches;
import com.roll_54.roll_mod.compat.mi.machines.OreWashers;
import com.roll_54.roll_mod.compat.mi.machines.ParallelMultiblocks;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.HatchMIHookContext;
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
        // Own name rather than the KubeJS material's casing name, so the two can never collide
        // whichever registers first.
        Casings.TUNGSTENSTEEL = MICasings.blockImitation(
                ParallelMultiblocks.CASING, "modern_industrialization:tungsten_steel_heat_proof_casing");
        Casings.TITANIUM_64 = MICasings.blockImitation(
                ParallelMultiblocks.TITANIUM_64_CASING, "modern_industrialization:titanium_64_machine_casing");
        Casings.CHROME_VANADIUM_STEEL = MICasings.blockImitation(
                ParallelMultiblocks.CHROME_VANADIUM_STEEL_CASING, "modern_industrialization:chrome_vanadium_steel_block");
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
        RecipeTypes.registerOwn();
        RecipeTypes.resolveExternal();
    }

    /** This mod's hatches, registered under {@code roll_mod:} -- see {@link FluidHatches}. */
    public static void hatches(HatchMIHookContext hook) {
        FluidHatches.register(hook);
    }

    /**
     * This mod's multiblock machines, registered under {@code roll_mod:} -- see
     * {@link ParallelMultiblocks}.
     *
     * <p>Runs from this mod's listener, which Tesseract calls after Extended Industrialization's (see
     * {@link #recipeTypes}), so EI's recipe types are bound by now.
     */
    public static void multiblocks(MultiblockMachinesMIHookContext hook) {
        ParallelMultiblocks.register(hook);
        CoilPyrolyseOven.register(hook);
        HighlyAdvancedDistillery.register(hook);
        OreWashers.register(hook);
    }

    //helpers
    public static final class Casings {
        public static MachineCasing THERMAL_VIBRATION_SAVE_CASING;

        public static MachineCasing SPACE_RESEARCH_STATION_CONTROLLER;
        public static MachineCasing SPACE_CASING_T2;
        public static MachineCasing INERT_CASING;
        public static MachineCasing TURBINE_CASING;
        public static MachineCasing ENDERIUM_CASING;
        public static MachineCasing TUNGSTENSTEEL;
        public static MachineCasing TITANIUM_64;
        public static MachineCasing CHROME_VANADIUM_STEEL;
    }

    public static final class RecipeTypes {
        public static MachineRecipeType BENDING_MACHINE;
        public static MachineRecipeType ALLOY_SMELTER;
        public static MachineRecipeType CANNING_MACHINE;
        public static MachineRecipeType COMPOSTER;
        public static MachineRecipeType BREWERY;
        public static MachineRecipeType PYROLYSE_OVEN;
        /** {@code lv_ore_washer}: the single-block washer's recipes, which the multiblock runs too. */
        public static MachineRecipeType ORE_WASHER;
        public static MachineRecipeType STEAM_ORE_WASHER;
        private static final Map<MachineRecipeType, String> RECIPE_TYPE_NAMES = Maps.newHashMap();

        public static Map<MachineRecipeType, String> getNames() {
            return RECIPE_TYPE_NAMES;
        }

        /**
         * Recipe types this mod brings itself, used to come from the pack's KubeJS startup script.
         *
         * <p>They keep their {@code modern_industrialization:} ids, so existing recipes and the
         * {@code event.recipes.modern_industrialization.*} KubeJS schemas still point at them, and are
         * created through {@link MIMachineRecipeTypes#create} -- the call KubeJS' own
         * {@code registerRecipeTypes} event makes. A type something else has registered already is
         * reused rather than registered twice.
         */
        static void registerOwn() {
            PYROLYSE_OVEN = existing("pyrolyse_oven");
            if (PYROLYSE_OVEN == null) {
                PYROLYSE_OVEN = MIMachineRecipeTypes.create("pyrolyse_oven")
                        .withItemInputs().withItemOutputs().withFluidInputs().withFluidOutputs();
            }
            ORE_WASHER = existing("lv_ore_washer");
            if (ORE_WASHER == null) {
                ORE_WASHER = MIMachineRecipeTypes.create("lv_ore_washer")
                        .withItemInputs().withFluidInputs().withItemOutputs();
            }
            STEAM_ORE_WASHER = existing("steam_ore_washer");
            if (STEAM_ORE_WASHER == null) {
                STEAM_ORE_WASHER = MIMachineRecipeTypes.create("steam_ore_washer")
                        .withItemInputs().withFluidInputs().withItemOutputs();
            }
        }

        /**
         * The recipe type {@code modern_industrialization:<path>} if something has registered it (the pack's
         * KubeJS script, for most of them), else null.
         */
        public static MachineRecipeType existing(String path) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("modern_industrialization", path);
            for (MachineRecipeType type : MIMachineRecipeTypes.getRecipeTypes()) {
                if (type.getId().equals(id)) {
                    return type;
                }
            }
            return null;
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
