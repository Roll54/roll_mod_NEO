package com.roll_54.roll_mod.economy.plot;

import com.roll_54.roll_mod.RollMod;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The explicit set of sellable plots in {@code roll_mod:shop_dim}, plus the map background they are
 * drawn over. Authoring a plot is one {@link Plot#of} line giving its two opposite world corners
 * (read off in-game with F3); the pixel overlay and covered chunks are derived automatically. Plots
 * may be any chunk-aligned rectangle — single chunk or many.
 */
public final class PlotRegistry {

  /** Top-down 1px-per-block map screenshot; its top-left pixel = world (-591.5, 464.5). */
  public static final ResourceLocation BACKGROUND =
      RollMod.id("textures/gui/xaero/2026-06-23_21.16.36_x-592_z464.png");

  /** Native pixel size of {@link #BACKGROUND} (also its draw size, since 1 px = 1 block). */
  public static final int IMG_W = 240;

  public static final int IMG_H = 240;

  // One Plot.of(id, startingPrice, x1,z1, x2,z2) per room. Corners are world X/Z (exclusive max so
  // each room is a full 16-block chunk); order does not matter; Y is ignored. startingPrice is the
  // plot's base price (escalates per-player in PlotService).
  private static final long DEFAULT_PRICE = 1000L;
  private static final long SMALL_CHUNK_PRICE = 1500L;
  private static final long BIG_CHUNK_PRICE = 10000L;

  // Sorted by chunk X ascending, then Z ascending.
  private static final List<Plot> PLOTS =
      List.of(
          // chunks (-35,31)..(-34,35) — 2x5
          Plot.of("plot_1", BIG_CHUNK_PRICE, -560.0, 496.0, -528.0, 576.0),
          // chunks (-35,37)..(-34,38) — 2x2
          Plot.of("plot_2", BIG_CHUNK_PRICE, -560.0, 592.0, -528.0, 624.0),
          // chunks (-35,40)..(-31,41) — 5x2
          Plot.of("plot_3", BIG_CHUNK_PRICE, -560.0, 640.0, -480.0, 672.0),
          // chunks (-32,31)..(-31,32) — 2x2
          Plot.of("plot_4", BIG_CHUNK_PRICE, -512.0, 496.0, -480.0, 528.0),
          // chunk (-32, 34)
          Plot.of("plot_5", DEFAULT_PRICE, -512.0, 544.0, -496.0, 560.0),
          // chunk (-32, 35)
          Plot.of("plot_6", DEFAULT_PRICE, -512.0, 560.0, -496.0, 576.0),
          // chunk (-32, 37)
          Plot.of("plot_7", DEFAULT_PRICE, -512.0, 592.0, -496.0, 608.0),
          // chunk (-32, 38)
          Plot.of("plot_8", DEFAULT_PRICE, -512.0, 608.0, -496.0, 624.0),
          // chunk (-31, 34)
          Plot.of("plot_9", DEFAULT_PRICE, -496.0, 544.0, -480.0, 560.0),
          // chunk (-31, 38)
          Plot.of("plot_10", DEFAULT_PRICE, -496.0, 608.0, -480.0, 624.0),
          // chunks (-29,31)..(-25,32) — 5x2
          Plot.of("plot_11", BIG_CHUNK_PRICE, -464.0, 496.0, -384.0, 528.0),
          // chunk (-29, 34)
          Plot.of("plot_12", DEFAULT_PRICE, -464.0, 544.0, -448.0, 560.0),
          // chunk (-29, 38)
          Plot.of("plot_13", DEFAULT_PRICE, -464.0, 608.0, -448.0, 624.0),
          // chunk (-28, 34)
          Plot.of("plot_14", DEFAULT_PRICE, -448.0, 544.0, -432.0, 560.0),
          // chunk (-28, 35)
          Plot.of("plot_15", DEFAULT_PRICE, -448.0, 560.0, -432.0, 576.0),
          // chunk (-28, 37)
          Plot.of("plot_16", DEFAULT_PRICE, -448.0, 592.0, -432.0, 608.0),
          // chunk (-28, 38)
          Plot.of("plot_17", DEFAULT_PRICE, -448.0, 608.0, -432.0, 624.0),
          // chunks (-26,34)..(-25,35) — 2x2
          Plot.of("plot_18", BIG_CHUNK_PRICE, -416.0, 544.0, -384.0, 576.0),
          // chunks (-26,37)..(-25,41) — 2x5
          Plot.of("plot_19", BIG_CHUNK_PRICE, -416.0, 592.0, -384.0, 672.0));

  private PlotRegistry() {}

  public static List<Plot> all() {
    return PLOTS;
  }

  public static Plot byId(String id) {
    for (Plot p : PLOTS) {
      if (p.id().equals(id)) {
        return p;
      }
    }
    return null;
  }
}
