package com.roll_54.roll_mod.economy.minprice;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.economy.minprice.PriceSolver.Entry;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.economy.vendingblock.gui.chat.Messages;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The minimal price table and the checks the auction and shop blocks run against it.
 *
 * <p>The table is built on a worker thread from a {@link RecipeSnapshot} taken on the server
 * thread, and swapped in whole once it is done. Until then the previous table keeps answering, so a
 * {@code /reload} never leaves a gap; a build that was overtaken by a newer one is thrown away.
 */
public final class MinPriceService {

  /** The last finished table; {@code null} before the first build or while the module is off. */
  private static volatile Table table;

  private static final AtomicInteger GENERATION = new AtomicInteger();

  /** How deep shulker boxes inside bundles inside shulker boxes are looked into. */
  private static final int CONTENTS_DEPTH = 4;

  private MinPriceService() {}

  public record Table(
      Map<String, Entry> entries,
      Set<String> noFloor,
      List<String> cycles,
      List<String> unresolved,
      RecipeSnapshot snapshot,
      SyncMinPricesPacket clientPacket,
      long builtMillis) {}

  public static boolean enabled() {
    return MinPriceConfig.MAIN != null && MinPriceConfig.MAIN.enabled;
  }

  public static Table table() {
    return table;
  }

  /* ------------------------------------------------- build ----------------------------------------------------- */

  /** Builds the table again. Call on the server thread; the solving itself runs off it. */
  public static CompletableFuture<Table> rebuild(MinecraftServer server) {
    int generation = GENERATION.incrementAndGet();
    if (!enabled()) {
      if (table != null) PacketDistributor.sendToAllPlayers(SyncMinPricesPacket.empty());
      table = null;
      return CompletableFuture.completedFuture(null);
    }
    if (!server.isSameThread()) {
      return CompletableFuture.supplyAsync(() -> rebuild(server), server).thenCompose(f -> f);
    }
    long start = System.currentTimeMillis();
    RecipeSnapshot snapshot;
    try {
      snapshot = RecipeSnapshot.take(server, MinPriceConfig.MAIN);
    } catch (Throwable t) {
      LOGGER.error("[MinPrice] could not read recipes, keeping the previous table", t);
      return CompletableFuture.failedFuture(t);
    }
    long snapped = System.currentTimeMillis();

    CompletableFuture<Table> built =
        CompletableFuture.supplyAsync(
            () -> {
              PriceSolver.Result result =
                  new PriceSolver(snapshot.recipes(), snapshot.fixed(), snapshot.outputWeights())
                      .solve();
              return new Table(
                  Map.copyOf(result.entries()),
                  Set.copyOf(snapshot.noFloor()),
                  List.copyOf(result.cycles()),
                  List.copyOf(result.unresolved()),
                  snapshot,
                  clientPacket(result.entries(), snapshot),
                  System.currentTimeMillis());
            },
            Util.backgroundExecutor());

    built.whenComplete(
        (done, error) ->
            server.execute(
                () -> {
                  if (error != null) {
                    LOGGER.error("[MinPrice] build failed, keeping the previous table", error);
                    return;
                  }
                  if (generation != GENERATION.get() || !enabled()) return; // overtaken
                  table = done;
                  PacketDistributor.sendToAllPlayers(done.clientPacket());
                  RecipeSnapshot.logProblems(snapshot.problems());
                  LOGGER.info(
                      "[MinPrice] {} prices from {} recipes in {} ms (snapshot {} ms on the server"
                          + " thread); {} cut from cycles, {} unresolved",
                      done.entries().size(),
                      snapshot.recipes().size(),
                      done.builtMillis() - start,
                      snapped - start,
                      done.cycles().size(),
                      done.unresolved().size());
                  LOGGER.info("[MinPrice] adapted: {}", RecipeSnapshot.summary(snapshot.adaptedByType()));
                  if (!snapshot.skippedByType().isEmpty()) {
                    LOGGER.info("[MinPrice] skipped: {}", RecipeSnapshot.summary(snapshot.skippedByType()));
                  }
                }));
    return built;
  }

  /** What the client shows in tooltips. Filled buckets are left out: their fluid is shown on its own. */
  private static SyncMinPricesPacket clientPacket(Map<String, Entry> entries, RecipeSnapshot snapshot) {
    Map<String, Double> items = new HashMap<>();
    Map<String, Double> fluids = new HashMap<>();
    entries.forEach(
        (key, entry) -> {
          if (entry.price() <= 0) return;
          if (key.startsWith(PriceSolver.FLUID_PREFIX)) {
            fluids.put(key.substring(PriceSolver.FLUID_PREFIX.length()), entry.price());
          } else if (!snapshot.noFloor().contains(key) && !snapshot.filledBuckets().contains(key)) {
            items.put(key, entry.price());
          }
        });
    return new SyncMinPricesPacket(Map.copyOf(items), Map.copyOf(fluids));
  }

  /** The table for a player who just joined; nothing while there is none. */
  public static void sendTo(ServerPlayer player) {
    Table t = table;
    if (t != null) PacketDistributor.sendToPlayer(player, t.clientPacket());
  }

  /** Drops the table and makes any build still running throw its result away. */
  public static void clear() {
    GENERATION.incrementAndGet();
    table = null;
  }

  /* ------------------------------------------------- lookup ---------------------------------------------------- */

  /**
   * Minimal price of one of {@code stack} (its count is ignored), including whatever it carries
   * inside: a shulker box full of diamonds is not sold for the price of the box.
   */
  public static double unitFloor(ItemStack stack) {
    Table t = table;
    return t == null ? 0 : unitFloor(t, stack, CONTENTS_DEPTH);
  }

  private static double unitFloor(Table t, ItemStack stack, int depth) {
    if (stack.isEmpty()) return 0;
    String key = RecipeSnapshot.itemKey(stack.getItem());
    double own = 0;
    if (!t.noFloor().contains(key)) {
      Entry entry = t.entries().get(key);
      if (entry != null) own = entry.price();
    }
    if (depth <= 0) return own;
    double inside = 0;
    ItemContainerContents container = stack.get(DataComponents.CONTAINER);
    if (container != null) {
      for (ItemStack in : container.nonEmptyItems()) inside += unitFloor(t, in, depth - 1) * in.getCount();
    }
    BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
    if (bundle != null) {
      for (ItemStack in : bundle.items()) inside += unitFloor(t, in, depth - 1) * in.getCount();
    }
    return own + inside;
  }

  /** {@code ceil(unit × count)}, worked out once for the whole lot and saturated, never per unit. */
  public static long floor(ItemStack stack, long count) {
    double total = unitFloor(stack) * Math.max(0, count);
    if (!(total > 0)) return 0;
    if (total >= Long.MAX_VALUE) return Long.MAX_VALUE;
    return (long) Math.ceil(total - 1e-9);
  }

  /* ------------------------------------------------- checks ---------------------------------------------------- */

  /**
   * Why {@code player} may not offer {@code count} of {@code stack} for {@code price}, or empty
   * when they may. Used where a listing or a shop position is created.
   */
  public static Optional<Component> check(ServerPlayer player, ItemStack stack, long count, long price) {
    if (!enabled() || LuckPermsCompat.canBypassMinPrice(player)) return Optional.empty();
    return checkPrice(stack, count, price);
  }

  /**
   * The same check without a player to ask for a bypass: an existing shop position at the moment
   * someone trades with it, which may have fallen under a floor raised since it was set.
   */
  public static Optional<Component> checkPrice(ItemStack stack, long count, long price) {
    if (!enabled()) return Optional.empty();
    if (price == 0 && MinPriceConfig.MAIN.allowFreePositions) return Optional.empty();
    if (table == null) {
      return MinPriceConfig.MAIN.blockSalesUntilReady
          ? Optional.of(Messages.minPriceNotReady())
          : Optional.empty();
    }
    long required = floor(stack, count);
    if (price >= required) return Optional.empty();
    return Optional.of(Messages.minPriceTooLow(stack.getHoverName(), count, required));
  }
}
