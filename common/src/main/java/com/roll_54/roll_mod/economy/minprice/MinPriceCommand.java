package com.roll_54.roll_mod.economy.minprice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.roll_54.roll_mod.economy.minprice.MinPriceService.Table;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Entry;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Input;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Output;
import com.roll_54.roll_mod.economy.minprice.PriceSolver.Recipe;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /rollmod minprice [item | hand | dump | rebuild]}: what the minimal price of something is
 * and which recipe made it so, the whole table to a file, or a fresh build.
 */
public final class MinPriceCommand {

  private MinPriceCommand() {}

  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
    dispatcher.register(
        Commands.literal("rollmod")
            .then(
                Commands.literal("minprice")
                    .requires(source -> source.hasPermission(2))
                    .then(
                        Commands.literal("hand")
                            .executes(
                                ctx ->
                                    show(
                                        ctx.getSource(),
                                        ctx.getSource().getPlayerOrException().getMainHandItem())))
                    .then(
                        Commands.literal("item")
                            .then(
                                Commands.argument("item", ItemArgument.item(context))
                                    .executes(
                                        ctx ->
                                            show(
                                                ctx.getSource(),
                                                ItemArgument.getItem(ctx, "item")
                                                    .createItemStack(1, false)))))
                    .then(Commands.literal("dump").executes(ctx -> dump(ctx.getSource())))
                    .then(Commands.literal("rebuild").executes(ctx -> rebuild(ctx.getSource())))));
  }

  private static int show(CommandSourceStack source, ItemStack stack) {
    Table table = MinPriceService.table();
    if (!ready(source, table)) return 0;
    String key = RecipeSnapshot.itemKey(stack.getItem());
    Entry entry = table.entries().get(key);
    if (entry == null || table.noFloor().contains(key)) {
      source.sendSuccess(() -> Component.literal(key + ": no minimal price"
          + (table.noFloor().contains(key) ? " (blacklisted)" : "")), false);
      return 1;
    }
    source.sendSuccess(
        () -> Component.literal(String.format("%s: %.2f per unit (%s%s)", key, entry.price(),
            entry.source(), entry.recipeId() == null ? "" : ", " + entry.recipeId())),
        false);
    Recipe recipe = find(table, entry.recipeId());
    if (recipe != null) {
      for (Input in : recipe.inputs()) {
        StringBuilder line = new StringBuilder("  - ").append(trim(in.count())).append(" × ");
        String cheapest = null;
        double best = Double.POSITIVE_INFINITY;
        for (String option : in.options()) {
          Entry e = table.entries().get(option);
          double p = e == null ? 0 : e.price();
          if (p < best) {
            best = p;
            cheapest = option;
          }
        }
        line.append(cheapest).append(String.format(" @ %.2f", best));
        if (in.options().size() > 1) line.append(" (cheapest of ").append(in.options().size()).append(")");
        source.sendSuccess(() -> Component.literal(line.toString()), false);
      }
      for (Output rem : recipe.remainders()) {
        source.sendSuccess(() -> Component.literal("  + returns " + rem.key()), false);
      }
      StringBuilder outs = new StringBuilder("  = ×").append(recipe.multiplier()).append(" →");
      for (Output out : recipe.outputs()) outs.append(' ').append(trim(out.count())).append(' ').append(out.key());
      source.sendSuccess(() -> Component.literal(outs.toString()), false);
    }
    long stackFloor = MinPriceService.floor(stack, stack.getMaxStackSize());
    source.sendSuccess(
        () -> Component.literal("  stack of " + stack.getMaxStackSize() + ": " + stackFloor), false);
    return 1;
  }

  private static int dump(CommandSourceStack source) {
    Table table = MinPriceService.table();
    if (!ready(source, table)) return 0;
    JsonObject root = new JsonObject();
    JsonObject prices = new JsonObject();
    for (Map.Entry<String, Entry> e : new TreeMap<>(table.entries()).entrySet()) {
      if (e.getValue().price() <= 0) continue;
      JsonObject row = new JsonObject();
      row.addProperty("price", e.getValue().price());
      row.addProperty("source", e.getValue().source().name());
      if (e.getValue().recipeId() != null) row.addProperty("recipe", e.getValue().recipeId());
      prices.add(e.getKey(), row);
    }
    root.add("prices", prices);
    root.add("cycles", array(table.cycles()));
    root.add("unresolved", array(table.unresolved()));
    root.add("problems", array(table.snapshot().problems()));
    root.add("adapted", MinestarFiles.GSON.toJsonTree(table.snapshot().adaptedByType()));
    root.add("skipped", MinestarFiles.GSON.toJsonTree(table.snapshot().skippedByType()));
    Path path = MinestarFiles.resolve("minprice_dump.json");
    MinestarFiles.write(path, root);
    source.sendSuccess(
        () -> Component.literal("Wrote " + prices.size() + " prices to " + path), false);
    return 1;
  }

  private static int rebuild(CommandSourceStack source) {
    if (!MinPriceService.enabled()) {
      source.sendFailure(Component.literal("Minimal prices are turned off (min_price config)."));
      return 0;
    }
    source.sendSuccess(() -> Component.literal("Rebuilding the minimal price table..."), false);
    MinPriceService.rebuild(source.getServer())
        .whenComplete(
            (table, error) ->
                source.getServer().execute(() -> {
                  if (error != null) source.sendFailure(Component.literal("Build failed: " + error));
                  else if (table != null)
                    source.sendSuccess(
                        () -> Component.literal("Built " + table.entries().size() + " prices."), true);
                }));
    return 1;
  }

  private static boolean ready(CommandSourceStack source, Table table) {
    if (!MinPriceService.enabled()) {
      source.sendFailure(Component.literal("Minimal prices are turned off (min_price config)."));
      return false;
    }
    if (table == null) {
      source.sendFailure(Component.literal("The minimal price table is still being built."));
      return false;
    }
    return true;
  }

  private static Recipe find(Table table, String id) {
    if (id == null) return null;
    for (Recipe recipe : table.snapshot().recipes()) if (recipe.id().equals(id)) return recipe;
    return null;
  }

  private static JsonArray array(Iterable<String> values) {
    JsonArray array = new JsonArray();
    values.forEach(array::add);
    return array;
  }

  private static String trim(double value) {
    return value == Math.rint(value) ? Long.toString((long) value) : String.format("%.3f", value);
  }
}
