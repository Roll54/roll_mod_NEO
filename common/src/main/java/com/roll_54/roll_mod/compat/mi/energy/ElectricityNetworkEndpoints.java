package com.roll_54.roll_mod.compat.mi.energy;

/** Implemented on MI's {@code ElectricityNetwork}: a node's machine connections changed. */
public interface ElectricityNetworkEndpoints {
    void roll_mod$markEndpointsDirty();
}
