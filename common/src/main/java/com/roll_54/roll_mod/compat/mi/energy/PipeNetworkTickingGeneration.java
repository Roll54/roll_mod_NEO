package com.roll_54.roll_mod.compat.mi.energy;

/**
 * Implemented on every MI {@code PipeNetwork}: counts how often MI rebuilt the network's ticking-node list (nodes added
 * or removed, networks split or merged, chunks starting or stopping to tick). A change means the node set changed.
 */
public interface PipeNetworkTickingGeneration {
    int roll_mod$tickingGeneration();
}
