package com.roll_54.roll_mod.compat.MBD2.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;

import java.util.List;

/**
 * Combines several {@link MIEnergyStorage} views into one, used when a machine exposes EU from
 * multiple parts on the same side. Analogous to MBD2's {@code EnergyStorageList} (FE).
 */
public record MergedMIEnergyStorage(List<MIEnergyStorage> storages) implements MIEnergyStorage {

    @Override
    public long receive(long maxReceive, boolean simulate) {
        long received = 0;
        for (MIEnergyStorage storage : storages) {
            received += storage.receive(maxReceive - received, simulate);
            if (received >= maxReceive) {
                break;
            }
        }
        return received;
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        long extracted = 0;
        for (MIEnergyStorage storage : storages) {
            extracted += storage.extract(maxExtract - extracted, simulate);
            if (extracted >= maxExtract) {
                break;
            }
        }
        return extracted;
    }

    @Override
    public long getAmount() {
        long amount = 0;
        for (MIEnergyStorage storage : storages) {
            amount += storage.getAmount();
        }
        return amount;
    }

    @Override
    public long getCapacity() {
        long capacity = 0;
        for (MIEnergyStorage storage : storages) {
            capacity += storage.getCapacity();
        }
        return capacity;
    }

    @Override
    public boolean canReceive() {
        return storages.stream().anyMatch(MIEnergyStorage::canReceive);
    }

    @Override
    public boolean canExtract() {
        return storages.stream().anyMatch(MIEnergyStorage::canExtract);
    }

    @Override
    public boolean canConnect(CableTier cableTier) {
        return storages.stream().anyMatch(storage -> storage.canConnect(cableTier));
    }
}
