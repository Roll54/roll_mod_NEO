package com.roll_54.roll_mod.economy.plot.client;

import com.roll_54.roll_mod.economy.plot.PlotSnapshot;
import java.util.List;

/**
 * Client-side snapshot of all plots for the viewing player, refreshed by {@code SyncPlotsPacket}.
 * {@code PlotPurchaseUI} reads from here when building/refreshing on the client (there is no synced
 * block entity to pull from). {@code volatile} because the netty thread writes it and the render
 * thread reads it.
 */
public final class ClientPlotCache {

  public static volatile List<PlotSnapshot> PLOTS = List.of();

  private ClientPlotCache() {}

  public static void set(List<PlotSnapshot> plots) {
    PLOTS = plots;
  }
}
