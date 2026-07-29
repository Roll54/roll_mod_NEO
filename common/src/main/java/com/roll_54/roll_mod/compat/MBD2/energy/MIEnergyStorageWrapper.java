package com.roll_54.roll_mod.compat.MBD2.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import com.lowdragmc.mbd2.api.capability.recipe.IO;

/**
 * View over {@link MIEnergyStore} handed to MI cables via {@code EnergyApi.SIDED}. Gates flow by the
 * trait's configured {@link IO} direction and per-transfer rate limits. Analogous to MBD2's
 * {@code EnergyStorageWrapper} (FE).
 */
public record MIEnergyStorageWrapper(MIEnergyStore storage, IO io, long maxReceive, long maxExtract)
        implements MIEnergyStorage {

    @Override
    public long receive(long maxReceive, boolean simulate) {
        if (io != IO.IN && io != IO.BOTH) {
            return 0;
        }
        return storage.receive(Math.min(this.maxReceive, maxReceive), simulate);
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        if (io != IO.OUT && io != IO.BOTH) {
            return 0;
        }
        return storage.extract(Math.min(this.maxExtract, maxExtract), simulate);
    }

    @Override
    public long getAmount() {
        return storage.getAmount();
    }

    @Override
    public long getCapacity() {
        return storage.getCapacity();
    }

    @Override
    public boolean canReceive() {
        return io == IO.IN || io == IO.BOTH;
    }

    @Override
    public boolean canExtract() {
        return io == IO.OUT || io == IO.BOTH;
    }

    @Override
    public boolean canConnect(CableTier cableTier) {
        return storage.canConnect(cableTier);
    }
}
