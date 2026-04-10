package com.roll_54.roll_mod.registry;

import aztech.modern_industrialization.machines.models.MachineCasing;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;

import com.google.common.collect.Maps;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.resources.ResourceLocation;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineCasingsMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineRecipeTypesMIHookContext;

import java.util.Map;
import java.util.function.Function;

import static com.roll_54.roll_mod.RollMod.MODID;
import static net.neoforged.neoforgespi.ILaunchContext.LOGGER;

public class MachineRegistry {


    public static void casings(MachineCasingsMIHookContext hook) {

        LOGGER.info("[{}]does in even work?.", MODID);
        MachineRegistry.Casings.THERMAL_VIBRATION_SAVE_CASING = hook.registerCubeAll("thermal_vibration_save_casing", "Test Bukvi", ResourceLocation.fromNamespaceAndPath(MODID, "block/casings/thermal_vibration_save_casing") );
    }





    //helpers
    public static final class Casings {
        public static MachineCasing THERMAL_VIBRATION_SAVE_CASING;
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
