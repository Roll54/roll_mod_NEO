package com.roll_54.roll_mod.compat.mi.energy;

/** Implemented on MI's {@code ElectricityNetworkNode}. */
public interface ElectricityNodeAccess {
    /** Whether this cable block touches at least one block it moves energy to or from. */
    boolean roll_mod$hasConnections();

    /** Drops the EU MI buffered in this cable block; cables no longer store energy. */
    void roll_mod$clearStoredEu();
}
