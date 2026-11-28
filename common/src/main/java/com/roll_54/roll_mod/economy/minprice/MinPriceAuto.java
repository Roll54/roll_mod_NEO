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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.Util;

/**
 * The server side of handset's "Auto", for every item and fluid at once: picks the shortest recipe for
 * each key and writes it to {@link MinPriceConfig#itemRecipes}, so the price is computed from that recipe.
 *
 * <p>Shortest is the same as in handset: the fewest different inputs, then the fewest input items per
 * item made; a recipe that takes the key itself is skipped, and one that only undoes another (ingot from
 * block, block from ingots) comes last. A recipe is only picked once all its inputs lead down to a price
 * (see {@link #pick}), so the picks never form a loop. Keys in {@link MinPriceConfig#autoBlacklist}, and keys with a fixed
 * price (ore tiers, whitelist, blacklist), are left alone.
 */
public final class MinPriceAuto {

  private MinPriceAuto() {}

  /** How many keys got a recipe, and how many were left out by the auto blacklist. */
  public record Result(int picked, int blacklisted) {}

  /** Set while an auto run is in flight, so two commands do not race on the config. */
  private static final AtomicBoolean RUNNING = new AtomicBoolean();

  /**
   * Picks the recipes off the server thread and writes them on it: only the snapshot (the recipe manager and the
   * tags are not safe elsewhere) and the config save run there. Completes on the server thread; {@code null} when a
   * run was already in progress.
   *
   * @param all every key (replacing recipes chosen before); otherwise only keys with no chosen recipe
   *     and no fixed price yet
   */
  public static CompletableFuture<Result> runAsync(MinecraftServer server, boolean all) {
    if (!RUNNING.compareAndSet(false, true)) return CompletableFuture.completedFuture(null);
    try {
      MinPriceConfig config = MinPriceConfig.MAIN;
      RecipeSnapshot snapshot = RecipeSnapshot.take(server, config);
      Set<String> blacklist = blacklist(config.autoBlacklist.get());
      Map<String, String> before = new LinkedHashMap<>(config.itemRecipes.get());
      return CompletableFuture
          .supplyAsync(() -> pick(snapshot, blacklist, before, all), Util.backgroundExecutor())
          .thenApplyAsync(picked -> {
            config.itemRecipes.validateAndSet(picked.chosen());
            ConfigApi.save(config);
            MinPriceService.rebuild(server);
            return picked.result();
          }, server)
          .whenComplete((r, t) -> RUNNING.set(false));
    } catch (Throwable t) {
      RUNNING.set(false);
      return CompletableFuture.failedFuture(t);
    }
  }

  private record Picked(Map<String, String> chosen, Result result) {}

  /**
   * Pure: works on the snapshot only, so it runs on a worker thread.
   *
   * <p>Grounded first: what has a fixed price, or that nothing makes (an ore, a mob drop). Then level by level, a
   * key is picked a recipe only from inputs grounded on an earlier level, so every chosen recipe leads down to
   * something with a price and no chosen recipes can close a loop among themselves. Keys never grounded keep no
   * chosen recipe and are left to the solver. Each recipe keeps a count of its inputs that have no grounded option
   * yet, and each key the inputs it is an option of, so grounding a key only touches the recipes that take it: one
   * pass over the graph instead of a full scan per level.
   */
  private static Picked pick(RecipeSnapshot snapshot, Set<String> blacklist, Map<String, String> before, boolean all) {
    List<Recipe> recipes = snapshot.recipes();
    Map<String, List<Recipe>> makers = new HashMap<>();
    for (Recipe recipe : recipes) {
      for (Output out : recipe.outputs()) makers.computeIfAbsent(out.key(), k -> new ArrayList<>()).add(recipe);
    }

    // "all" starts over, so recipes picked earlier (a loop among them included) do not linger.
    Map<String, String> chosen = all ? new LinkedHashMap<>() : new LinkedHashMap<>(before);
    int picked = 0;
    int skipped = 0;
    for (String key : makers.keySet()) {
      if (snapshot.fixed().containsKey(key) || !blacklist.contains(key)) continue;
      skipped++;
      if (all) chosen.remove(key);
    }

    // missing[i]: inputs of recipe i with no grounded option yet; satisfied[i][j]: input j has one.
    int n = recipes.size();
    int[] missing = new int[n];
    boolean[][] satisfied = new boolean[n][];
    Map<String, List<int[]>> takenBy = new HashMap<>(); // key -> {recipe, input} it is an option of
    for (int i = 0; i < n; i++) {
      List<Input> inputs = recipes.get(i).inputs();
      missing[i] = inputs.size();
      satisfied[i] = new boolean[inputs.size()];
      for (int j = 0; j < inputs.size(); j++) {
        for (String option : inputs.get(j).options()) {
          takenBy.computeIfAbsent(option, k -> new ArrayList<>()).add(new int[] {i, j});
        }
      }
    }
    Map<Recipe, Integer> index = new IdentityHashMap<>(n);
    for (int i = 0; i < n; i++) index.put(recipes.get(i), i);

    Set<String> grounded = new HashSet<>();
    List<String> level = new ArrayList<>();
    for (String key : snapshot.fixed().keySet()) if (grounded.add(key)) level.add(key);
    for (String key : takenBy.keySet()) if (!makers.containsKey(key) && grounded.add(key)) level.add(key);

    while (!level.isEmpty()) {
      // Ground this level: recipes whose last open input it fills become ready, and their outputs are next.
      Set<String> candidates = new TreeSet<>();
      for (String key : level) {
        for (int[] use : takenBy.getOrDefault(key, List.of())) {
          int r = use[0];
          if (satisfied[r][use[1]]) continue;
          satisfied[r][use[1]] = true;
          if (--missing[r] == 0) {
            for (Output out : recipes.get(r).outputs()) if (!grounded.contains(out.key())) candidates.add(out.key());
          }
        }
      }
      List<String> next = new ArrayList<>();
      for (String key : candidates) {
        List<Recipe> ready = new ArrayList<>();
        for (Recipe recipe : makers.get(key)) {
          if (!recipe.inputs().isEmpty() && missing[index.get(recipe)] == 0) ready.add(recipe);
        }
        if (ready.isEmpty()) continue;
        next.add(key);
        // Blacklisted, or (for "missing") already given a recipe: grounded, but not picked for.
        if (blacklist.contains(key) || (!all && snapshot.forcedRecipes().containsKey(key))) continue;
        Recipe best = shortest(key, ready, makers);
        if (best == null) continue;
        chosen.put(key, best.id());
        picked++;
      }
      grounded.addAll(next);
      level = next;
    }
    return new Picked(chosen, new Result(picked, skipped));
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
