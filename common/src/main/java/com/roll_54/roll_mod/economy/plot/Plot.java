package com.roll_54.roll_mod.economy.plot;

import java.util.List;
import net.minecraft.world.level.ChunkPos;

/**
 * A sellable plot in {@code roll_mod:shop_dim}. Authored from two opposite world corners via {@link
 * #of}; the overlay pixel rectangle (on the map image) and the covered chunk set are derived once
 * at construction. The {@link #anchor()} chunk is used as the ownership probe. {@link
 * #startingPrice()} is this plot's base price, before the per-player escalation applied in {@code
 * PlotService}.
 */
public record Plot(
    String id,
    long startingPrice,
    int pixelX,
    int pixelY,
    int pixelW,
    int pixelH,
    List<ChunkPos> chunks) {

  /** The chunk used to test ownership of the whole plot (the min-corner chunk). */
  public ChunkPos anchor() {
    return chunks.get(0);
  }

  /**
   * Build a plot from two opposite world corners (Y is ignored — claims are full-height columns).
   * The corners may be given in any order. {@code startingPrice} is the plot's base price.
   */
  public static Plot of(String id, long startingPrice, double x1, double z1, double x2, double z2) {
    double minX = Math.min(x1, x2);
    double maxX = Math.max(x1, x2);
    double minZ = Math.min(z1, z2);
    double maxZ = Math.max(z1, z2);
    int px = PlotMath.pixelX(minX);
    int py = PlotMath.pixelY(minZ);
    int pw = (int) Math.round(maxX - minX);
    int ph = (int) Math.round(maxZ - minZ);
    return new Plot(
        id, startingPrice, px, py, pw, ph, PlotMath.chunksInRect(minX, minZ, maxX, maxZ));
  }
}
