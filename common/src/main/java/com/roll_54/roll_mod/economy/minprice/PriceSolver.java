package com.roll_54.roll_mod.economy.minprice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Works out the minimal price of every item and fluid from a recipe graph.
 *
 * <p>Pure Java on purpose: it only sees string keys and numbers, so it runs on a worker thread
 * without touching a registry, a tag or a recipe object. Keys are item ids ({@code
 * minecraft:iron_ingot}) and fluid ids prefixed with {@link #FLUID_PREFIX}; fluid amounts are in
 * buckets, so a fluid's price is always per B.
 *
 * <p>A price is written once and never changes again ("frozen"). That is what keeps recursive
 * recipes (ingot ⇄ block, packing and unpacking) from growing a price forever while the most
 * expensive recipe wins: the first recipe to price one side of a cycle fixes it, and the other side
 * is priced from that.
 */
public final class PriceSolver {

  public static final String FLUID_PREFIX = "fluid:";

  /** One ingredient slot: any of {@code options} (cheapest one counts), {@code count} times. */
  public record Input(List<String> options, double count) {}

  /** One product, {@code count} being the expected amount (count × chance). */
  public record Output(String key, double count) {}

  public record Recipe(
      String id,
      String type,
      double multiplier,
      List<Input> inputs,
      List<Output> outputs,
      List<Output> remainders) {}

  public enum Source {
    /** Whitelisted, an ore tier or blacklisted: given, not computed. */
    FIXED,
    /** No usable recipe makes it: no floor. */
    LEAF,
    /** Every recipe of it was priced and the most expensive one won. */
    RECIPE,
    /** Part of a cycle: priced from the recipes that could be priced when the cycle was cut. */
    CYCLE,
    /** Part of a cycle that nothing outside it leads into: no floor. */
    UNRESOLVED
  }

  /** A key's price and why it has it. */
  public record Entry(double price, Source source, String recipeId) {}

  public record Result(Map<String, Entry> entries, List<String> cycles, List<String> unresolved) {}

  private final List<Recipe> recipes;
  private final Map<String, Double> fixed;
  private final Map<String, Double> outputWeights;
  private final Map<String, String> forcedRecipes;
  private final double minimum;

  /**
   * @param recipes every usable recipe; blacklisted ones are already left out
   * @param fixed prices that are given and never computed (whitelist, ore tiers, blacklist at 0)
   * @param outputWeights share of a recipe's cost each output takes per unit; missing means 1
   * @param forcedRecipes key → the only recipe its price is computed from (config {@code itemRecipes})
   * @param minimum the absolute minimal price of one unit: what an item no recipe makes is worth. It is
   *     not applied to computed prices, which stay exact so cheap inputs (planks, sticks, water at 0) and
   *     recipe multipliers carry through; the minimum is enforced only where a unit is sold
   *     ({@link MinPriceService#unitFloor}, {@link ClientMinPrices#unit}).
   */
  public PriceSolver(
      Collection<Recipe> recipes, Map<String, Double> fixed, Map<String, Double> outputWeights,
      Map<String, String> forcedRecipes, double minimum) {
    this.forcedRecipes = forcedRecipes;
    this.minimum = Math.max(0, minimum);
    this.recipes = new ArrayList<>(recipes);
    this.recipes.sort(Comparator.comparing(Recipe::id));
    this.fixed = fixed;
    this.outputWeights = outputWeights;
  }

  public Result solve() {
    Map<String, Entry> frozen = new HashMap<>();
    fixed.forEach((key, price) -> frozen.put(key, new Entry(sanitize(price), Source.FIXED, null)));

    List<Recipe> netted = new ArrayList<>(recipes.size());
    for (Recipe recipe : recipes) {
      Recipe net = netSelfLoops(recipe);
      if (net != null) netted.add(net);
    }

    // Which recipes can make each key, in recipe-id order.
    Map<String, List<Recipe>> makers = new TreeMap<>();
    TreeSet<String> keys = new TreeSet<>();
    for (Recipe recipe : netted) {
      for (Output out : recipe.outputs()) {
        makers.computeIfAbsent(out.key(), k -> new ArrayList<>()).add(recipe);
        keys.add(out.key());
      }
      for (Input in : recipe.inputs()) keys.addAll(in.options());
      for (Output rem : recipe.remainders()) keys.add(rem.key());
    }

    // Every recipe of each key, before the chosen ones narrow it down; the same lists, so a key without a
    // chosen recipe has the identical list in both maps.
    Map<String, List<Recipe>> allMakers = new HashMap<>(makers);

    // A key with a chosen recipe (config itemRecipes) is priced from that recipe alone.
    forcedRecipes.forEach((key, recipeId) -> {
      List<Recipe> all = makers.get(key);
      if (all == null) return;
      List<Recipe> chosen = all.stream().filter(r -> r.id().equals(recipeId)).toList();
      if (!chosen.isEmpty()) makers.put(key, new ArrayList<>(chosen));
    });

    for (String key : keys) {
      if (!frozen.containsKey(key) && !makers.containsKey(key)) {
        frozen.put(key, new Entry(minimum, Source.LEAF, null));
      }
    }

    List<String> cycles = new ArrayList<>();
    List<String> unresolved = new ArrayList<>();
    TreeSet<String> open = new TreeSet<>();
    for (String key : makers.keySet()) if (!frozen.containsKey(key)) open.add(key);

    while (!open.isEmpty()) {
      // Strict: only once every recipe of a key can be priced, so the maximum is the real one.
      boolean progress = true;
      while (progress) {
        progress = false;
        for (var it = open.iterator(); it.hasNext(); ) {
          String key = it.next();
          Best best = best(key, makers.get(key), frozen, true);
          if (best != null) {
            frozen.put(key, new Entry(best.price, Source.RECIPE, best.recipeId));
            it.remove();
            progress = true;
          }
        }
      }
      if (open.isEmpty()) break;

      // Relaxed: what is left waits on itself. Cut every cycle at once with the recipes that can
      // be priced right now; the values are worked out before any is written, so the order the
      // keys are visited in cannot change the answer.
      Map<String, Best> cut = new LinkedHashMap<>();
      for (String key : open) {
        Best best = best(key, makers.get(key), frozen, false);
        if (best != null) cut.put(key, best);
      }
      // A chosen recipe stuck in a loop of chosen recipes: let the key's other recipes in.
      if (cut.isEmpty()) {
        for (String key : open) {
          List<Recipe> every = allMakers.get(key);
          if (every == makers.get(key)) continue;
          Best best = best(key, every, frozen, false);
          if (best != null) cut.put(key, best);
        }
      }
      if (cut.isEmpty()) {
        // Still stuck: some loop has no way in at all. Only the loops that wait on nothing but
        // themselves get the minimum; everything made from them is then priced from that, instead
        // of everything left over falling to the minimum together.
        for (String key : stuckLoops(open, allMakers)) {
          frozen.put(key, new Entry(minimum, Source.UNRESOLVED, null));
          unresolved.add(key);
          open.remove(key);
        }
        continue;
      }
      cut.forEach(
          (key, best) -> {
            frozen.put(key, new Entry(best.price, Source.CYCLE, best.recipeId));
            cycles.add(key);
            open.remove(key);
          });
    }
    return new Result(frozen, cycles, unresolved);
  }

  private record Best(double price, String recipeId) {}

  /**
   * The open keys in loops that depend on no other open key: the strongly connected components of the
   * open keys' "needs" graph (key → the open keys its recipes take) with no edge leaving them. There is
   * always at least one while anything is open, so the solver keeps moving.
   */
  private static List<String> stuckLoops(Set<String> open, Map<String, List<Recipe>> makers) {
    Map<String, Set<String>> needs = new HashMap<>();
    for (String key : open) {
      Set<String> deps = new HashSet<>();
      for (Recipe recipe : makers.getOrDefault(key, List.of())) {
        for (Input in : recipe.inputs()) {
          for (String option : in.options()) if (open.contains(option)) deps.add(option);
        }
      }
      needs.put(key, deps);
    }
    // Tarjan, iterative, so a long chain cannot overflow the stack.
    Map<String, Integer> index = new HashMap<>();
    Map<String, Integer> low = new HashMap<>();
    Map<String, Integer> component = new HashMap<>();
    List<List<String>> components = new ArrayList<>();
    ArrayDeque<String> stack = new ArrayDeque<>();
    Set<String> onStack = new HashSet<>();
    int counter = 0;
    for (String start : new TreeSet<>(open)) {
      if (index.containsKey(start)) continue;
      ArrayDeque<Iterator<String>> work = new ArrayDeque<>();
      ArrayDeque<String> path = new ArrayDeque<>();
      index.put(start, counter);
      low.put(start, counter++);
      stack.push(start);
      onStack.add(start);
      path.push(start);
      work.push(needs.get(start).iterator());
      while (!path.isEmpty()) {
        String node = path.peek();
        Iterator<String> it = work.peek();
        if (it.hasNext()) {
          String next = it.next();
          if (!index.containsKey(next)) {
            index.put(next, counter);
            low.put(next, counter++);
            stack.push(next);
            onStack.add(next);
            path.push(next);
            work.push(needs.get(next).iterator());
          } else if (onStack.contains(next)) {
            low.put(node, Math.min(low.get(node), index.get(next)));
          }
          continue;
        }
        path.pop();
        work.pop();
        if (!path.isEmpty()) low.put(path.peek(), Math.min(low.get(path.peek()), low.get(node)));
        if (low.get(node).equals(index.get(node))) {
          List<String> members = new ArrayList<>();
          String member;
          do {
            member = stack.pop();
            onStack.remove(member);
            component.put(member, components.size());
            members.add(member);
          } while (!member.equals(node));
          components.add(members);
        }
      }
    }
    List<String> stuck = new ArrayList<>();
    for (List<String> members : components) {
      int id = component.get(members.get(0));
      boolean closed = members.stream().allMatch(key -> needs.get(key).stream().allMatch(dep -> component.get(dep) == id));
      if (closed) stuck.addAll(members);
    }
    return stuck;
  }

  /**
   * The most expensive of {@code makers} for {@code key}, or {@code null} when it cannot be
   * decided yet. Strict needs every maker priceable with every option of every input known;
   * relaxed takes whatever makers can be priced from the options that are known.
   */
  private Best best(String key, List<Recipe> makers, Map<String, Entry> frozen, boolean strict) {
    Best best = null;
    for (Recipe recipe : makers) {
      double cost = cost(recipe, frozen, strict);
      if (Double.isNaN(cost)) {
        if (strict) return null;
        continue;
      }
      double unit = unitPrice(recipe, key, cost);
      if (best == null || unit > best.price) best = new Best(unit, recipe.id());
    }
    return best;
  }

  /** The recipe's total cost after its multiplier, or NaN while an input is not priced yet. */
  private double cost(Recipe recipe, Map<String, Entry> frozen, boolean strict) {
    double total = 0;
    for (Input in : recipe.inputs()) {
      double cheapest = Double.POSITIVE_INFINITY;
      for (String option : in.options()) {
        Entry entry = frozen.get(option);
        if (entry == null) {
          if (strict) return Double.NaN;
          continue;
        }
        cheapest = Math.min(cheapest, entry.price());
      }
      if (cheapest == Double.POSITIVE_INFINITY) return Double.NaN;
      total += cheapest * in.count();
    }
    for (Output rem : recipe.remainders()) {
      Entry entry = frozen.get(rem.key());
      if (entry == null) return Double.NaN;
      total -= entry.price() * rem.count();
    }
    return Math.max(0, total) * recipe.multiplier();
  }

  /**
   * One unit's share of {@code cost}. Each output takes {@code weight × count} of the whole, so
   * the shares add up to exactly the cost, and the share is then divided over the units it covers.
   */
  private double unitPrice(Recipe recipe, String key, double cost) {
    double weighted = 0;
    double count = 0;
    for (Output out : recipe.outputs()) {
      weighted += weight(out.key()) * out.count();
      if (out.key().equals(key)) count += out.count();
    }
    if (weighted <= 0 || count <= 0) return 0;
    return sanitize(cost * weight(key) / weighted);
  }

  private double weight(String key) {
    Double w = outputWeights.get(key);
    return w == null ? 1.0 : Math.max(0, w);
  }

  /**
   * Takes out what a recipe gives back of its own input (a catalyst that comes out again, an
   * MI recipe returning part of what it ate), so the recipe does not wait on its own output. Only
   * inputs with a single option are netted; a recipe left with no outputs is dropped.
   */
  private static Recipe netSelfLoops(Recipe recipe) {
    Map<String, Double> in = new HashMap<>();
    for (Input input : recipe.inputs()) {
      if (input.options().size() == 1) in.merge(input.options().get(0), input.count(), Double::sum);
    }
    Map<String, Double> out = new LinkedHashMap<>();
    for (Output output : recipe.outputs()) out.merge(output.key(), output.count(), Double::sum);
    boolean touched = false;
    for (String key : in.keySet()) {
      if (out.containsKey(key)) {
        touched = true;
        double both = Math.min(in.get(key), out.get(key));
        in.put(key, in.get(key) - both);
        out.put(key, out.get(key) - both);
      }
    }
    if (!touched) return recipe;

    List<Input> inputs = new ArrayList<>();
    for (Input input : recipe.inputs()) {
      if (input.options().size() != 1) {
        inputs.add(input);
        continue;
      }
      String key = input.options().get(0);
      Double left = in.get(key);
      if (left != null && left > 1e-9) {
        inputs.add(new Input(input.options(), left));
        in.put(key, 0.0); // merged into this one entry
      }
    }
    List<Output> outputs = new ArrayList<>();
    out.forEach((key, count) -> {
      if (count > 1e-9) outputs.add(new Output(key, count));
    });
    if (outputs.isEmpty()) return null;
    return new Recipe(
        recipe.id(), recipe.type(), recipe.multiplier(), inputs, outputs, recipe.remainders());
  }

  private static final double CAP = Long.MAX_VALUE / 64.0;

  private static double sanitize(double value) {
    if (Double.isNaN(value) || value <= 0) return 0;
    return Math.min(value, CAP);
  }
}
