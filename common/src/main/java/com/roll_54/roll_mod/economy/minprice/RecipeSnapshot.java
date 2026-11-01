package com.roll_54.roll_mod.economy.minprice;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import aztech.modern_industrialization.blocks.forgehammer.ForgeHammerRecipe;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Input;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Output;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Recipe;
import com.roll_54.roll_mod.mixin.SmithingTransformRecipeAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.crafting.ingredient.ChanceResult;

/**
 * Everything the solver needs, copied out of the live game on the server thread.
 *
 * <p>Ingredients resolve their tags lazily and cache the result, and {@code /reload} swaps the
 * recipe manager and the tags underneath — neither is safe to read from another thread. So recipes,
 * tags and config are turned into plain strings and numbers here, and the worker only ever sees
 * this copy.
 */
public record RecipeSnapshot(
    List<Recipe> recipes,
    Map<String, Double> fixed,
    Map<String, Double> outputWeights,
    Set<String> noFloor,
    Set<String> filledBuckets,
    Map<String, Integer> adaptedByType,
    Map<String, Integer> skippedByType,
    List<String> problems,
    Map<String, String> forcedRecipes,
    double minimum) {

  /** A solver over this snapshot, with the config's chosen recipes and absolute minimum. */
  public PriceSolver solver() {
    return new PriceSolver(recipes, fixed, outputWeights, forcedRecipes, minimum);
  }

  /** Must run on the server thread. */
  public static RecipeSnapshot take(MinecraftServer server, MinPriceConfig config) {
    List<String> problems = new ArrayList<>();
    Map<String, Integer> adapted = new TreeMap<>();
    Map<String, Integer> skipped = new TreeMap<>();

    // ---- config: fixed prices
    Map<String, ? extends Long> tierPrices = config.tiers.get();
    Map<String, Double> fixed = new HashMap<>();
    config.oreTiers.get().forEach((ref, tier) -> {
      Long price = tierPrices.get(tier);
      if (price == null) {
        problems.add("oreTiers: '" + ref + "' names unknown tier '" + tier + "'");
        return;
      }
      for (String item : items(ref, "oreTiers", problems)) fixed.put(item, price.doubleValue());
    });
    config.itemWhitelist.get().forEach((ref, price) -> {
      for (String item : items(ref, "itemWhitelist", problems)) fixed.put(item, price.doubleValue());
    });
    config.fluidWhitelist.get().forEach((id, price) -> {
      ResourceLocation rl = ResourceLocation.tryParse(id);
      if (rl == null || !BuiltInRegistries.FLUID.containsKey(rl)) {
        problems.add("fluidWhitelist: unknown fluid '" + id + "'");
        return;
      }
      fixed.put(fluidKey(BuiltInRegistries.FLUID.get(rl)), price.doubleValue());
    });
    Set<String> noFloor = new LinkedHashSet<>();
    for (String ref : config.itemBlacklist.get()) {
      for (String item : items(ref, "itemBlacklist", problems)) {
        if (fixed.containsKey(item) && fixed.get(item) > 0) {
          problems.add("itemBlacklist: '" + item + "' is also priced in oreTiers/itemWhitelist; blacklist wins");
        }
        fixed.put(item, 0.0);
        noFloor.add(item);
      }
    }
    Map<String, Double> weights = new HashMap<>();
    config.outputWeights.get().forEach((ref, w) -> {
      for (String item : items(ref, "outputWeights", problems)) weights.put(item, w);
    });

    // ---- recipes
    List<? extends String> blacklist = config.recipeBlacklist.get();
    Map<String, ? extends Double> multipliers = config.recipeTypeMultipliers.get();
    double defaultMultiplier = config.defaultTypeMultiplier.get();
    List<Recipe> recipes = new ArrayList<>();
    for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
      String id = holder.id().toString();
      ResourceLocation typeRl = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
      String type = typeRl == null ? "unknown" : typeRl.toString();
      if (blacklisted(id, blacklist)) {
        skipped.merge(type + " (blacklisted)", 1, Integer::sum);
        continue;
      }
      Double multiplier = multipliers.get(type);
      if (multiplier == null) {
        if (config.skipUnknownTypes) {
          skipped.merge(type + " (unknown type)", 1, Integer::sum);
          continue;
        }
        multiplier = defaultMultiplier;
      }
      Recipe recipe;
      try {
        recipe = switch (holder.value()) {
          case MachineRecipe mi -> fromMachine(id, type, multiplier, mi);
          case ForgeHammerRecipe hammer -> fromHammer(id, type, multiplier, hammer);
          case SmithingTransformRecipe smithing -> fromSmithing(id, type, multiplier, smithing);
          case MBDRecipe mbd -> fromMbd(id, type, multiplier, mbd);
          case InscriberRecipe inscriber -> fromInscriber(id, type, multiplier, inscriber);
          case CuttingBoardRecipe cutting -> fromCuttingBoard(id, type, multiplier, cutting);
          case CookingPotRecipe pot -> fromCookingPot(id, type, multiplier, pot, server);
          default -> fromGeneric(id, type, multiplier, holder.value(), server);
        };
      } catch (Throwable t) {
        problems.add("recipe " + id + " could not be read: " + t);
        recipe = null;
      }
      if (recipe == null) {
        skipped.merge(type, 1, Integer::sum);
      } else {
        recipes.add(recipe);
        adapted.merge(type, 1, Integer::sum);
      }
    }

    // ---- filled buckets: one bucket of the fluid plus the empty bucket.
    String emptyBucket = itemKey(Items.BUCKET);
    Set<String> filledBuckets = new LinkedHashSet<>();
    for (Item item : BuiltInRegistries.ITEM) {
      if (!(item instanceof BucketItem bucket) || item == Items.BUCKET) continue;
      Fluid fluid = bucket.content;
      if (fluid == null || fluid == Fluids.EMPTY) continue;
      filledBuckets.add(itemKey(item));
      recipes.add(new Recipe(
          "roll_mod:minprice/fill/" + itemKey(item).replace(':', '/'),
          "roll_mod:bucket_fill",
          1.0,
          List.of(new Input(List.of(emptyBucket), 1), new Input(List.of(fluidKey(fluid)), 1)),
          List.of(new Output(itemKey(item), 1)),
          List.of()));
      adapted.merge("roll_mod:bucket_fill", 1, Integer::sum);
    }

    // ---- chosen recipes: checked against what was actually read, so a typo is reported, not ignored.
    Map<String, String> forced = new LinkedHashMap<>();
    config.itemRecipes.get().forEach((ref, recipeId) -> {
      List<String> keys;
      if (ref.trim().startsWith(PriceSolver.FLUID_PREFIX)) {
        ResourceLocation rl = ResourceLocation.tryParse(ref.trim().substring(PriceSolver.FLUID_PREFIX.length()));
        if (rl == null || !BuiltInRegistries.FLUID.containsKey(rl)) {
          problems.add("itemRecipes: unknown fluid '" + ref + "'");
          return;
        }
        keys = List.of(fluidKey(BuiltInRegistries.FLUID.get(rl)));
      } else {
        keys = items(ref, "itemRecipes", problems);
      }
      Recipe recipe = recipes.stream().filter(r -> r.id().equals(recipeId)).findFirst().orElse(null);
      if (recipe == null) {
        problems.add("itemRecipes: '" + ref + "' -> recipe '" + recipeId
            + "' does not exist, is blacklisted or could not be read; its usual recipes are used");
        return;
      }
      for (String key : keys) {
        if (recipe.outputs().stream().noneMatch(o -> o.key().equals(key))) {
          problems.add("itemRecipes: recipe '" + recipeId + "' does not make '" + key + "'; its usual recipes are used");
        } else {
          forced.put(key, recipeId);
        }
      }
    });

    return new RecipeSnapshot(
        recipes, fixed, weights, noFloor, filledBuckets, adapted, skipped, problems, forced,
        config.absoluteMinPrice.get());
  }

  /* ------------------------------------------------ adapters --------------------------------------------------- */

  /**
   * Any recipe that reports its ingredients and result the vanilla way. {@code isSpecial()} is not a
   * reason to skip: AE2's charger/transform and many mod machines set it only to stay out of the
   * recipe book. Truly dynamic recipes (map cloning, firework stars...) report no result and are
   * skipped by that instead.
   */
  private static Recipe fromGeneric(
      String id, String type, double multiplier, net.minecraft.world.item.crafting.Recipe<?> recipe,
      MinecraftServer server) {
    ItemStack result = recipe.getResultItem(server.registryAccess());
    if (result == null || result.isEmpty()) return null;

    List<Input> inputs = new ArrayList<>();
    List<Output> remainders = new ArrayList<>();
    for (Ingredient ingredient : recipe.getIngredients()) {
      if (ingredient == null || ingredient.isEmpty()) continue; // a blank crafting-grid slot
      List<String> options = options(ingredient);
      if (options.isEmpty()) return null; // a tag nothing is in: the recipe cannot be made
      inputs.add(new Input(options, 1));
      String remainder = sharedRemainder(ingredient);
      if (remainder != null) remainders.add(new Output(remainder, 1));
    }
    if (inputs.isEmpty()) return null; // inputs we cannot see (e.g. smithing): unknown, not free
    return new Recipe(
        id, type, multiplier, inputs, List.of(new Output(itemKey(result.getItem()), result.getCount())),
        remainders);
  }

  /** Marked special (it has its own screen), and the input has a count the generic path ignores. */
  private static Recipe fromHammer(String id, String type, double multiplier, ForgeHammerRecipe recipe) {
    List<String> options = options(recipe.ingredient());
    if (options.isEmpty() || recipe.result().isEmpty()) return null;
    return new Recipe(
        id, type, multiplier, List.of(new Input(options, recipe.count())),
        List.of(new Output(itemKey(recipe.result().getItem()), recipe.result().getCount())),
        List.of());
  }

  /** {@code getIngredients()} is empty for smithing; all three slots are used up. */
  private static Recipe fromSmithing(
      String id, String type, double multiplier, SmithingTransformRecipe recipe) {
    SmithingTransformRecipeAccessor slots = (SmithingTransformRecipeAccessor) recipe;
    ItemStack result = slots.roll_mod$getResult();
    if (result == null || result.isEmpty()) return null;
    List<Input> inputs = new ArrayList<>();
    for (Ingredient ingredient :
        List.of(slots.roll_mod$getTemplate(), slots.roll_mod$getBase(), slots.roll_mod$getAddition())) {
      if (ingredient == null || ingredient.isEmpty()) continue;
      List<String> options = options(ingredient);
      if (options.isEmpty()) return null;
      inputs.add(new Input(options, 1));
    }
    if (inputs.isEmpty()) return null;
    return new Recipe(
        id, type, multiplier, inputs,
        List.of(new Output(itemKey(result.getItem()), result.getCount())), List.of());
  }

  private static Recipe fromMachine(String id, String type, double multiplier, MachineRecipe recipe) {
    List<Input> inputs = new ArrayList<>();
    for (MachineRecipe.ItemInput in : recipe.itemInputs) {
      if (in.probability() <= 0) continue; // catalyst: never consumed
      List<String> options = options(in.ingredient());
      if (options.isEmpty()) return null;
      inputs.add(new Input(options, in.amount() * (double) in.probability()));
    }
    for (MachineRecipe.FluidInput in : recipe.fluidInputs) {
      if (in.probability() <= 0) continue;
      List<String> options = new ArrayList<>();
      for (Fluid fluid : in.getInputFluids()) options.add(fluidKey(fluid));
      if (options.isEmpty()) return null;
      inputs.add(new Input(dedupe(options), in.amount() / 1000.0 * in.probability()));
    }
    List<Output> outputs = new ArrayList<>();
    for (MachineRecipe.ItemOutput out : recipe.itemOutputs) {
      // Chanced outputs (byproducts) are left out entirely: they take no share of the cost and
      // the recipe does not price them. Otherwise a byproduct many ores drop takes the most
      // expensive ore's share and ends up dearer than the main product of its own chain.
      if (out.probability() < 1) continue;
      ItemStack stack = out.getStack();
      if (stack.isEmpty()) continue;
      outputs.add(new Output(itemKey(stack.getItem()), out.amount()));
    }
    for (MachineRecipe.FluidOutput out : recipe.fluidOutputs) {
      if (out.probability() < 1) continue; // chanced, as above
      outputs.add(new Output(fluidKey(out.fluid()), out.amount() / 1000.0));
    }
    if (outputs.isEmpty()) return null;
    return new Recipe(id, type, multiplier, inputs, outputs, List.of());
  }

  /**
   * MBD2 machines (roll_mod's own multiblocks): items and fluids per capability. A per-tick input
   * runs for the whole duration; chance 0 is a catalyst (never consumed); chanced outputs are left
   * out like MI's byproducts. Energy and other capabilities are not priced.
   */
  private static Recipe fromMbd(String id, String type, double multiplier, MBDRecipe recipe) {
    List<Input> inputs = new ArrayList<>();
    for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
      if (content.chance <= 0 || !(content.content instanceof SizedIngredient sized)) continue;
      List<String> options = options(sized.ingredient());
      if (options.isEmpty()) return null;
      inputs.add(new Input(options, sized.count() * (double) Math.min(1f, content.chance) * ticks(content, recipe)));
    }
    for (Content content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
      if (content.chance <= 0 || !(content.content instanceof SizedFluidIngredient sized)) continue;
      List<String> options = new ArrayList<>();
      for (FluidStack fluid : sized.getFluids()) options.add(fluidKey(fluid.getFluid()));
      if (options.isEmpty()) return null;
      inputs.add(new Input(dedupe(options),
          sized.amount() / 1000.0 * Math.min(1f, content.chance) * ticks(content, recipe)));
    }
    List<Output> outputs = new ArrayList<>();
    for (Content content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
      if (content.chance < 1 || !(content.content instanceof SizedIngredient sized)) continue;
      ItemStack[] stacks = sized.ingredient().getItems();
      if (stacks.length == 0 || stacks[0].isEmpty()) continue;
      outputs.add(new Output(itemKey(stacks[0].getItem()), sized.count() * ticks(content, recipe)));
    }
    for (Content content : recipe.getOutputContents(FluidRecipeCapability.CAP)) {
      if (content.chance < 1 || !(content.content instanceof SizedFluidIngredient sized)) continue;
      FluidStack[] fluids = sized.getFluids();
      if (fluids.length == 0) continue;
      outputs.add(new Output(fluidKey(fluids[0].getFluid()), sized.amount() / 1000.0 * ticks(content, recipe)));
    }
    if (outputs.isEmpty()) return null;
    return new Recipe(id, type, multiplier, inputs, outputs, List.of());
  }

  private static int ticks(Content content, MBDRecipe recipe) {
    return content.perTick ? Math.max(1, recipe.duration) : 1;
  }

  /** AE2 inscriber: in INSCRIBE mode the top and bottom presses stay in the machine. */
  private static Recipe fromInscriber(String id, String type, double multiplier, InscriberRecipe recipe) {
    ItemStack result = recipe.getResultItem();
    if (result.isEmpty()) return null;
    List<Ingredient> used = new ArrayList<>(List.of(recipe.getMiddleInput()));
    if (recipe.getProcessType() == InscriberProcessType.PRESS) {
      used.add(recipe.getTopOptional());
      used.add(recipe.getBottomOptional());
    }
    List<Input> inputs = new ArrayList<>();
    for (Ingredient ingredient : used) {
      if (ingredient == null || ingredient.isEmpty()) continue;
      List<String> options = options(ingredient);
      if (options.isEmpty()) return null;
      inputs.add(new Input(options, 1));
    }
    if (inputs.isEmpty()) return null;
    return new Recipe(id, type, multiplier, inputs,
        List.of(new Output(itemKey(result.getItem()), result.getCount())), List.of());
  }

  /** Farmer's Delight cutting board: the knife is not used up; only the sure results count. */
  private static Recipe fromCuttingBoard(String id, String type, double multiplier, CuttingBoardRecipe recipe) {
    List<String> options = recipe.getIngredients().isEmpty() ? List.of() : options(recipe.getIngredients().get(0));
    if (options.isEmpty()) return null;
    List<Output> outputs = new ArrayList<>();
    for (ChanceResult result : recipe.getRollableResults()) {
      if (result.chance() < 1 || result.stack().isEmpty()) continue;
      outputs.add(new Output(itemKey(result.stack().getItem()), result.stack().getCount()));
    }
    if (outputs.isEmpty()) return null;
    return new Recipe(id, type, multiplier, List.of(new Input(options, 1)), outputs, List.of());
  }

  /** Farmer's Delight cooking pot: the generic reading plus the bowl/bottle the meal is served in. */
  private static Recipe fromCookingPot(
      String id, String type, double multiplier, CookingPotRecipe recipe, MinecraftServer server) {
    Recipe base = fromGeneric(id, type, multiplier, recipe, server);
    if (base == null) return null;
    ItemStack container = recipe.getOutputContainer();
    if (container == null || container.isEmpty()) return base;
    List<Input> inputs = new ArrayList<>(base.inputs());
    inputs.add(new Input(List.of(itemKey(container.getItem())), container.getCount()));
    return new Recipe(id, type, multiplier, inputs, base.outputs(), base.remainders());
  }

  /* ------------------------------------------------- helpers --------------------------------------------------- */

  private static List<String> options(Ingredient ingredient) {
    List<String> options = new ArrayList<>();
    for (ItemStack stack : ingredient.getItems()) {
      if (!stack.isEmpty()) options.add(itemKey(stack.getItem()));
    }
    return dedupe(options);
  }

  /** The item every option of {@code ingredient} leaves in the grid (a bucket of milk → bucket). */
  private static String sharedRemainder(Ingredient ingredient) {
    String shared = null;
    for (ItemStack stack : ingredient.getItems()) {
      if (!stack.hasCraftingRemainingItem()) return null;
      ItemStack left = stack.getCraftingRemainingItem();
      if (left.isEmpty()) return null;
      String key = itemKey(left.getItem());
      if (shared != null && !shared.equals(key)) return null;
      shared = key;
    }
    return shared;
  }

  private static List<String> dedupe(List<String> list) {
    return List.copyOf(new LinkedHashSet<>(list));
  }

  /** An item id or {@code #tag} from the config, as item keys; unknown ones are reported. */
  private static List<String> items(String ref, String field, List<String> problems) {
    String trimmed = ref.trim();
    if (trimmed.startsWith("#")) {
      ResourceLocation rl = ResourceLocation.tryParse(trimmed.substring(1));
      if (rl == null) {
        problems.add(field + ": bad tag '" + ref + "'");
        return List.of();
      }
      List<String> out = new ArrayList<>();
      BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, rl))
          .ifPresent(set -> set.forEach(holder -> out.add(itemKey(holder.value()))));
      if (out.isEmpty()) problems.add(field + ": tag '" + ref + "' is empty or unknown");
      return out;
    }
    ResourceLocation rl = ResourceLocation.tryParse(trimmed);
    if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
      problems.add(field + ": unknown item '" + ref + "'");
      return List.of();
    }
    return List.of(rl.toString());
  }

  static boolean blacklisted(String id, List<? extends String> blacklist) {
    for (String entry : blacklist) {
      String e = entry.trim();
      if (e.endsWith("*") ? id.startsWith(e.substring(0, e.length() - 1)) : id.equals(e)) return true;
    }
    return false;
  }

  static String itemKey(Item item) {
    return BuiltInRegistries.ITEM.getKey(item).toString();
  }

  /** Flowing and source variants are the same fluid as far as a price goes. */
  static String fluidKey(Fluid fluid) {
    Fluid source = fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    return PriceSolver.FLUID_PREFIX + BuiltInRegistries.FLUID.getKey(source);
  }

  /** Recipe count per type, for the log line. */
  static String summary(Map<String, Integer> byType) {
    Map<String, Integer> sorted = new LinkedHashMap<>();
    byType.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .forEach(e -> sorted.put(e.getKey(), e.getValue()));
    return sorted.toString();
  }

  static void logProblems(List<String> problems) {
    for (String problem : problems) LOGGER.warn("[MinPrice] {}", problem);
  }
}
