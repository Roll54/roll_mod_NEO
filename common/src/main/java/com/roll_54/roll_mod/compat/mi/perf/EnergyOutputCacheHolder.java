package com.roll_54.roll_mod.compat.mi.perf;

import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jspecify.annotations.Nullable;

/**
 * Implemented on every MI {@code MachineBlockEntity}: the cached energy target of {@code EnergyHelper.autoOutput}
 * (generators, transformers, storage units pushing into the block in front of them).
 */
public interface EnergyOutputCacheHolder {
    @Nullable
    BlockCapabilityCache<MIEnergyStorage, Direction> roll_mod$energyOutputCache();

    void roll_mod$setEnergyOutputCache(BlockCapabilityCache<MIEnergyStorage, Direction> cache);
}
