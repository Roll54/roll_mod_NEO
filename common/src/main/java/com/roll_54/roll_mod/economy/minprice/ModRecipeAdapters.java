package com.roll_54.roll_mod.economy.minprice;

import com.roll_54.roll_mod.economy.minprice.PriceSolver.Input;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Output;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Recipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;

/**
 * Recipes of mods whose recipe classes report no ingredients or no result the vanilla way, so the generic
 * reading of {@link RecipeSnapshot} skipped them: Ad Astra's machines, Brewin' and Chewin's keg and Create
 * Cybernetics' engineering table. Each mod's classes sit in their own nested class and are only touched once that
 * mod is loaded. Fluids are in buckets, like everywhere in the solver.
 */
final class ModRecipeAdapters {

  private static final boolean AD_ASTRA = ModList.get().isLoaded("ad_astra");
  private static final boolean BREWIN = ModList.get().isLoaded("brewinandchewin");
  private static final boolean CYBERNETICS = ModList.get().isLoaded("createcybernetics");

  private ModRecipeAdapters() {}

  /** The recipe read by its mod's adapter, or {@code null} when no adapter knows it (or it cannot be made). */
  static Recipe adapt(String id, String type, double multiplier, net.minecraft.world.item.crafting.Recipe<?> recipe) {
    if (AD_ASTRA) {
      Recipe r = AdAstra.adapt(id, type, multiplier, recipe);
      if (r != null) return r;
    }
    if (BREWIN) {
      Recipe r = Brewin.adapt(id, type, multiplier, recipe);
      if (r != null) return r;
    }
    if (CYBERNETICS) return Cybernetics.adapt(id, type, multiplier, recipe);
    return null;
  }

  /** Every ingredient once; {@code null} when one of them matches nothing. */
  private static List<Input> each(List<Ingredient> ingredients) {
    List<Input> inputs = new ArrayList<>();
    for (Ingredient ingredient : ingredients) {
      if (ingredient == null || ingredient.isEmpty()) continue;
      List<String> options = RecipeSnapshot.options(ingredient);
      if (options.isEmpty()) return null;
      inputs.add(new Input(options, 1));
    }
    return inputs;
  }

  private static Recipe of(String id, String type, double multiplier, List<Input> inputs, List<Output> outputs) {
    if (inputs == null || inputs.isEmpty() || outputs.isEmpty()) return null;
    return new Recipe(id, type, multiplier, inputs, outputs, List.of());
  }

  private static List<Output> item(ItemStack stack) {
    if (stack == null || stack.isEmpty()) return List.of();
    return List.of(new Output(RecipeSnapshot.itemKey(stack.getItem()), stack.getCount()));
  }

  private static List<Output> fluid(Fluid fluid, double millibuckets) {
    if (fluid == null || fluid == Fluids.EMPTY || millibuckets <= 0) return List.of();
    return List.of(new Output(RecipeSnapshot.fluidKey(fluid), millibuckets / 1000.0));
  }

  /* ------------------------------------------------- Ad Astra ------------------------------------------------- */

  private static final class AdAstra {
    static Recipe adapt(String id, String type, double multiplier, net.minecraft.world.item.crafting.Recipe<?> recipe) {
      return switch (recipe) {
        case earth.terrarium.adastra.common.recipes.machines.CompressingRecipe r ->
            of(id, type, multiplier, each(List.of(r.ingredient())), item(r.result()));
        case earth.terrarium.adastra.common.recipes.machines.AlloyingRecipe r ->
            of(id, type, multiplier, each(r.ingredients()), item(r.result()));
        case earth.terrarium.adastra.common.recipes.machines.NasaWorkbenchRecipe r ->
            of(id, type, multiplier, each(r.ingredients()), item(r.result()));
        case earth.terrarium.adastra.common.recipes.machines.CryoFreezingRecipe r ->
            of(id, type, multiplier, each(List.of(r.input())),
                fluid(r.result().resource().getType(), r.result().amount()));
        case earth.terrarium.adastra.common.recipes.machines.RefiningRecipe r ->
            of(id, type, multiplier, fluidInput(r.input()), fluid(r.result().resource().getType(), r.result().amount()));
        case earth.terrarium.adastra.common.recipes.machines.OxygenLoadingRecipe r ->
            of(id, type, multiplier, fluidInput(r.input()), fluid(r.result().resource().getType(), r.result().amount()));
        default -> null;
      };
    }

    private static List<Input> fluidInput(
        earth.terrarium.common_storage_lib.resources.fluid.ingredient.SizedFluidIngredient sized) {
      List<String> options = new ArrayList<>();
      for (var stack : sized.getFluids()) {
        Fluid fluid = stack.resource().getType();
        if (fluid != null && fluid != Fluids.EMPTY) options.add(RecipeSnapshot.fluidKey(fluid));
      }
      if (options.isEmpty()) return null;
      return List.of(new Input(RecipeSnapshot.dedupe(options), sized.getAmountAsMb() / 1000.0));
    }
  }

  /* --------------------------------------------- Brewin' and Chewin' ------------------------------------------ */

  private static final class Brewin {
    static Recipe adapt(String id, String type, double multiplier, net.minecraft.world.item.crafting.Recipe<?> recipe) {
      if (!(recipe instanceof umpaz.brewinandchewin.common.crafting.KegFermentingRecipe keg)) return null;
      List<Input> inputs = each(keg.getIngredients());
      if (inputs == null) return null;
      inputs = new ArrayList<>(inputs);
      if (keg.getFluidIngredient().isPresent()) {
        var wanted = keg.getFluidIngredient().get();
        List<String> options = new ArrayList<>();
        for (var stack : wanted.ingredient().displayStacks()) {
          if (stack.fluid() != null && stack.fluid() != Fluids.EMPTY) options.add(RecipeSnapshot.fluidKey(stack.fluid()));
        }
        if (options.isEmpty()) return null;
        inputs.add(new Input(RecipeSnapshot.dedupe(options), wanted.loaderAmount() / 1000.0));
      }
      List<Output> outputs = keg.getResult().map(
          stack -> stack == null || stack.isEmpty() ? List.<Output>of()
              : fluid(stack.fluid(), millibuckets(stack)),
          ModRecipeAdapters::item);
      return of(id, type, multiplier, inputs, outputs);
    }

    private static double millibuckets(umpaz.brewinandchewin.common.utility.AbstractedFluidStack stack) {
      return stack.unit() == null ? stack.amount()
          : umpaz.brewinandchewin.common.utility.FluidUnit.convertToLoader(stack.amount(), stack.unit());
    }
  }

  /* --------------------------------------------- Create Cybernetics ------------------------------------------- */

  private static final class Cybernetics {
    /** Taking a part apart at the engineering table: every listed piece comes out; there is no chance in it. */
    static Recipe adapt(String id, String type, double multiplier, net.minecraft.world.item.crafting.Recipe<?> recipe) {
      if (!(recipe instanceof com.perigrine3.createcybernetics.recipe.EngineeringDeconstructRecipe r)) return null;
      List<Output> outputs = new ArrayList<>();
      for (ItemStack stack : r.outputs()) outputs.addAll(item(stack));
      return of(id, type, multiplier, each(r.inputs()), outputs);
    }
  }
}
