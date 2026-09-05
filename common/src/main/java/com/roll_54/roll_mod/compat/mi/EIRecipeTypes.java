package com.roll_54.roll_mod.compat.mi;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.MachineRegistry;
import net.swedz.extended_industrialization.EIMachines;

/**
 * Binds Extended Industrialization's machine recipe types into {@link MachineRegistry.RecipeTypes}.
 *
 * <p>Every reference to EI lives in this class and nowhere else, so it is only ever class-loaded
 * behind a {@code ModList.get().isLoaded("extended_industrialization")} check -- the same shape as
 * {@code com.roll_54.roll_mod.compat.ftb.FTBTeamsFacadeImpl}. Touching {@link EIMachines} from a
 * class that loads unconditionally would turn a missing EI into a NoClassDefFoundError during mod
 * construction.
 *
 * <p>EI fills these fields from its own {@code MIHookListener#machineRecipeTypes}, so this is only
 * safe to call from a listener that Tesseract runs after EI's -- see
 * {@link MachineRegistry#recipeTypes}.
 */
public final class EIRecipeTypes {

    private EIRecipeTypes() {}

    public static void bind() {
        MachineRegistry.RecipeTypes.ALLOY_SMELTER = check("alloy_smelter", EIMachines.RecipeTypes.ALLOY_SMELTER);
        MachineRegistry.RecipeTypes.BENDING_MACHINE = check("bending_machine", EIMachines.RecipeTypes.BENDING_MACHINE);
        MachineRegistry.RecipeTypes.CANNING_MACHINE = check("canning_machine", EIMachines.RecipeTypes.CANNING_MACHINE);
        MachineRegistry.RecipeTypes.COMPOSTER = check("composter", EIMachines.RecipeTypes.COMPOSTER);
        MachineRegistry.RecipeTypes.BREWERY = check("brewery", EIMachines.RecipeTypes.BREWERY);
    }

    private static <T> T check(String name, T type) {
        if (type == null) {
            // Means this ran before EI's listener after all, which the mods.toml ordering is there
            // to prevent -- worth saying out loud rather than leaving a null to surface later.
            RollMod.LOGGER.warn("[MI] Extended Industrialization has not registered '{}' yet.", name);
        }
        return type;
    }
}
