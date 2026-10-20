package com.roll_54.roll_mod.economy.minprice;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import aztech.modern_industrialization.blocks.forgehammer.ForgeHammerRecipe;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
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
    List<String> problems) {

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

    return new RecipeSnapshot(
        recipes, fixed, weights, noFloor, filledBuckets, adapted, skipped, problems);
  }

  /* ------------------------------------------------ adapters --------------------------------------------------- */

  private static Recipe fromGeneric(
      String id, String type, double multiplier, net.minecraft.world.item.crafting.Recipe<?> recipe,
      MinecraftServer server) {
    if (recipe.isSpecial()) return null;
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
