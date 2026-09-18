package com.roll_54.roll_mod.economy.plot;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

/**
 * Calibration and chunk math for the {@code roll_mod:shop_dim} plot map.
 *
 * <p>The background image is a 1:1 (one pixel per block) top-down Xaero screenshot whose top-left
 * pixel corresponds to world coordinate {@link #MAP_ORIGIN_X}/{@link #MAP_ORIGIN_Z}. So a world
 * (x,z) maps to image pixel {@code (x - MAP_ORIGIN_X, z - MAP_ORIGIN_Z)}.
 */
public final class PlotMath {

  /** World X at image pixel column 0. */
  public static final double MAP_ORIGIN_X = -591.5;

  /** World Z at image pixel row 0. */
  public static final double MAP_ORIGIN_Z = 464.5;

  private PlotMath() {}

  /** Image pixel column for a world X (1 px = 1 block). */
  public static int pixelX(double worldX) {
    return (int) Math.round(worldX - MAP_ORIGIN_X);
  }

  /** Image pixel row for a world Z (1 px = 1 block). */
  public static int pixelY(double worldZ) {
    return (int) Math.round(worldZ - MAP_ORIGIN_Z);
  }

  /**
   * Every chunk overlapped by the world rectangle spanning {@code [minX,maxX] x [minZ,maxZ]}. The
   * upper bounds are treated as exclusive (a corner that lands exactly on a chunk border does not
   * pull in the next chunk). The first element is the min-corner chunk, used as the plot's anchor.
   */
  public static List<ChunkPos> chunksInRect(double minX, double minZ, double maxX, double maxZ) {
    int cx0 = Mth.floor(minX) >> 4;
    int cz0 = Mth.floor(minZ) >> 4;
    int cx1 = Mth.floor(maxX - 1.0e-4) >> 4;
    int cz1 = Mth.floor(maxZ - 1.0e-4) >> 4;
    List<ChunkPos> out = new ArrayList<>();
    for (int cx = cx0; cx <= cx1; cx++) {
      for (int cz = cz0; cz <= cz1; cz++) {
        out.add(new ChunkPos(cx, cz));
      }
    }
    return out;
  }
}
