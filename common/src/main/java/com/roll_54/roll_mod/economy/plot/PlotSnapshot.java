package com.roll_54.roll_mod.economy.plot;

import javax.annotation.Nullable;

/**
 * A per-viewer view of one plot, computed server-side and synced to the client so the GUI can
 * render without any async work. Everything is baked for the receiving player: {@code price} is
 * what THIS viewer would pay (the escalated amount) and {@code affordable} is balance &ge; price at
 * sync time.
 *
 * <ul>
 *   <li>{@code VACANT} — unowned, buyable from the server at the escalated base price.
 *   <li>{@code MINE} — owned by the viewer; shows resale / sell-to-server controls.
 *   <li>{@code OTHER_LISTED} — owned by someone else and listed for resale; buyable.
 *   <li>{@code OTHER_UNLISTED} — owned by someone else, not for sale.
 * </ul>
 */
public record PlotSnapshot(
    String id,
    int pixelX,
    int pixelY,
    int pixelW,
    int pixelH,
    Kind kind,
    @Nullable String ownerName,
    long price,
    boolean affordable,
    long startingPrice,
    boolean listed,
    long listingPrice) {

  public enum Kind {
    VACANT,
    MINE,
    OTHER_LISTED,
    OTHER_UNLISTED
  }

  /** Buyable by the viewer (server sale or someone else's resale listing). */
  public boolean buyable() {
    return kind == Kind.VACANT || kind == Kind.OTHER_LISTED;
  }
}
