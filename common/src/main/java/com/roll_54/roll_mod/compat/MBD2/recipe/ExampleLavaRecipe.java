package com.roll_54.roll_mod.compat.MBD2.recipe;

import com.lowdragmc.mbd2.api.recipe.MBDRecipeBuilder;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Worked example: a recipe type plus one recipe that consumes EU + water and produces lava.
 *
 * <p>EU is expressed through MBD2's built-in {@code forge_energy} recipe capability
 * ({@link ForgeEnergyRecipeCapability#CAP}); our {@code mi_energy_storage} trait's recipe handler
 * consumes it from the machine's EU buffer 1:1 (see {@code MIEnergyTrait.MIEnergyRecipeHandler}).
 *
 * <p>This example is NOT registered by default. To enable it, register an instance on the mod event
 * bus, e.g. in {@code RollMod}'s constructor:
 * <pre>{@code eventBus.register(new ExampleLavaRecipe());}</pre>
 *
 * <p>For the recipe to actually run, a machine built in the MBD2 editor must use the
 * {@link #LAVA_SMELTERY} recipe type and carry the matching traits (MI EU IN, water fluid tank IN,
 * lava fluid tank OUT). See {@code docs/mbd2-recipes.md}.
 */
public class ExampleLavaRecipe {

    /** Registry id of the recipe type. A machine references this to run its recipes. */
    public static final ResourceLocation LAVA_SMELTERY = RollMod.id("lava_smeltery");

    @SubscribeEvent
    public void onRegisterRecipeType(MBDRegistryEvent.MBDRecipeType event) {
        // 1) Create the recipe type. (Alternatively load one authored in the editor:
        //    event.registerFromResource(getClass(), "roll_mod/recipe_types/lava_smeltery.rt");)
        MBDRecipeType lavaSmeltery = new MBDRecipeType(LAVA_SMELTERY);
        lavaSmeltery.setXEIVisible(true);

        // 2) Add the EU + water -> lava recipe as a built-in recipe (no datapack needed).
        MBDRecipeBuilder.of(RollMod.id("water_to_lava"), lavaSmeltery)
                .duration(200)                                        // 200 ticks (10 s)
                .perTick(true)
                .input(ForgeEnergyRecipeCapability.CAP, 32)           // 32 EU per tick -> 6400 EU total
                .perTick(false)
                .inputFluids(new FluidStack(Fluids.WATER, 1000))      // 1000 mB water, consumed once
                .outputFluids(new FluidStack(Fluids.LAVA, 1000))      // 1000 mB lava
                .saveAsBuiltinRecipe();

        // 3) Register the recipe type so machines can reference it.
        event.register(lavaSmeltery);
        RollMod.LOGGER.info("[{}] registered example MBD2 recipe type '{}'", RollMod.MODID, LAVA_SMELTERY);
    }
}
