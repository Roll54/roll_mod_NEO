package com.roll_54.roll_mod.economy.minprice;

import com.roll_54.roll_mod.economy.minprice.PriceSolver.Input;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Output;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Recipe;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The server side of handset's "Auto", for every item and fluid at once: picks the shortest recipe for
 * each key and writes it to {@link MinPriceConfig#itemRecipes}, so the price is computed from that recipe.
 *
 * <p>Shortest is the same as in handset: the fewest different inputs, then the fewest input items per
 * item made; a recipe that takes the key itself is skipped, and one that only undoes another (ingot from
 * block, block from ingots) comes last. A recipe is only picked once all its inputs lead down to a price
 * (see {@link #run}), so the picks never form a loop. Keys in {@link MinPriceConfig#autoBlacklist}, and keys with a fixed
 * price (ore tiers, whitelist, blacklist), are left alone.
 */
public final class MinPriceAuto {

  private MinPriceAuto() {}

  /** How many keys got a recipe, and how many were left out by the auto blacklist. */
  public record Result(int picked, int blacklisted) {}

  /**
   * @param all every key (replacing recipes chosen before); otherwise only keys with no chosen recipe
   *     and no fixed price yet
   */
  public static Result run(MinecraftServer server, boolean all) {
    MinPriceConfig config = MinPriceConfig.MAIN;
    RecipeSnapshot snapshot = RecipeSnapshot.take(server, config);
    Set<String> blacklist = blacklist(config.autoBlacklist.get());

    Map<String, List<Recipe>> makers = new TreeMap<>();
    for (Recipe recipe : snapshot.recipes()) {
      for (Output out : recipe.outputs()) makers.computeIfAbsent(out.key(), k -> new ArrayList<>()).add(recipe);
    }

    // "all" starts over, so recipes picked earlier (a loop among them included) do not linger.
    Map<String, String> chosen = all ? new LinkedHashMap<>() : new LinkedHashMap<>(config.itemRecipes.get());
    int picked = 0;
    int skipped = 0;
    for (String key : makers.keySet()) {
      if (snapshot.fixed().containsKey(key) || !blacklist.contains(key)) continue;
      skipped++;
      if (all) chosen.remove(key);
    }

    // Grounded first: what has a fixed price, or that nothing makes (an ore, a mob drop). Then round by
    // round, a key is picked a recipe only from inputs grounded in an earlier round, so every chosen
    // recipe leads down to something with a price and no chosen recipes can close a loop among
    // themselves (which used to leave whole branches at the minimum). Keys never grounded keep no chosen
    // recipe and are left to the solver.
    Set<String> grounded = new HashSet<>(snapshot.fixed().keySet());
    for (Recipe recipe : snapshot.recipes()) {
      for (Input in : recipe.inputs()) {
        for (String option : in.options()) if (!makers.containsKey(option)) grounded.add(option);
      }
    }
    boolean progress = true;
    while (progress) {
      progress = false;
      List<String> reached = new ArrayList<>();
      for (Map.Entry<String, List<Recipe>> entry : makers.entrySet()) {
        String key = entry.getKey();
        if (grounded.contains(key)) continue;
        List<Recipe> ready = entry.getValue().stream().filter(r -> fromGrounded(r, grounded)).toList();
        if (ready.isEmpty()) continue;
        reached.add(key);
        // Blacklisted, or (for "missing") already given a recipe: grounded, but not picked for.
        if (blacklist.contains(key) || (!all && snapshot.forcedRecipes().containsKey(key))) continue;
        Recipe best = shortest(key, ready, makers);
        if (best == null) continue;
        chosen.put(key, best.id());
        picked++;
      }
      if (!reached.isEmpty()) {
        grounded.addAll(reached);
        progress = true;
      }
    }

    config.itemRecipes.validateAndSet(chosen);
    ConfigApi.save(config);
    MinPriceService.rebuild(server);
    return new Result(picked, skipped);
  }

  /** Every input of the recipe can be had from a grounded option. */
  private static boolean fromGrounded(Recipe recipe, Set<String> grounded) {
    for (Input in : recipe.inputs()) {
      if (in.options().stream().noneMatch(grounded::contains)) return false;
    }
    return !recipe.inputs().isEmpty();
  }

  private static Recipe shortest(String key, List<Recipe> candidates, Map<String, List<Recipe>> makers) {
    Recipe best = null;
    double[] bestScore = null;
    for (Recipe recipe : candidates) {
      double made = recipe.outputs().stream().filter(o -> o.key().equals(key)).mapToDouble(Output::count).sum();
      if (made <= 0 || recipe.inputs().isEmpty()) continue;
      if (recipe.inputs().stream().anyMatch(in -> in.options().contains(key))) continue;
      double items = recipe.inputs().stream().mapToDouble(in -> Math.max(1, in.count())).sum();
      double[] score = {undoes(recipe, key, makers) ? 1 : 0, recipe.inputs().size(), items / made};
      if (bestScore == null || compare(score, bestScore) < 0) {
        best = recipe;
        bestScore = score;
      }
    }
    return best;
  }

  /** Whether one of the recipe's inputs is itself made from {@code key}: the recipe just reverses another. */
  private static boolean undoes(Recipe recipe, String key, Map<String, List<Recipe>> makers) {
    for (Input input : recipe.inputs()) {
      if (input.options().size() != 1) continue;
      for (Recipe back : makers.getOrDefault(input.options().get(0), List.of())) {
        if (back != recipe && back.inputs().stream().anyMatch(in -> in.options().contains(key))) return true;
      }
    }
    return false;
  }

  private static int compare(double[] a, double[] b) {
    for (int i = 0; i < a.length; i++) {
      int c = Double.compare(a[i], b[i]);
      if (c != 0) return c;
    }
    return 0;
  }

  /**
   * The table keys of {@code refs}: item ids, {@code #item tags}, fluid ids (plain, when no item has that
   * id, like {@code minecraft:water}, or as {@code fluid:<id>}), and {@code #fluid:<tag>} for fluid tags.
   */
  static Set<String> blacklist(List<? extends String> refs) {
    Set<String> keys = new HashSet<>();
    for (String raw : refs) {
      String ref = raw.trim();
      if (ref.startsWith("#fluid:")) {
        ResourceLocation rl = ResourceLocation.tryParse(ref.substring("#fluid:".length()));
        if (rl != null) BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, rl)).ifPresent(set ->
            set.forEach(holder -> keys.add(PriceSolver.FLUID_PREFIX + BuiltInRegistries.FLUID.getKey(holder.value()))));
      } else if (ref.startsWith("#")) {
        ResourceLocation rl = ResourceLocation.tryParse(ref.substring(1));
        if (rl != null) BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, rl)).ifPresent(set ->
            set.forEach(holder -> keys.add(BuiltInRegistries.ITEM.getKey(holder.value()).toString())));
      } else if (ref.startsWith(PriceSolver.FLUID_PREFIX)) {
        keys.add(ref);
      } else {
        ResourceLocation rl = ResourceLocation.tryParse(ref);
        if (rl == null) continue;
        if (BuiltInRegistries.ITEM.containsKey(rl)) keys.add(rl.toString());
        if (BuiltInRegistries.FLUID.containsKey(rl)) keys.add(PriceSolver.FLUID_PREFIX + rl);
      }
    }
    return keys;
  }
}
