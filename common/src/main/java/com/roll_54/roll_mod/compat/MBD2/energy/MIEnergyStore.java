package com.roll_54.roll_mod.compat.MBD2.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Backing EU buffer for {@link MIEnergyTrait}. Mirrors MBD2's own
 * {@code CopiableEnergyStorage} (FE) but exposes native MI EU via {@link MIEnergyStorage}.
 *
 * <p>Internally the value is an {@code int} (inherited from NeoForge {@link EnergyStorage}) so the
 * store is losslessly persisted/synced through LDLib's {@code INBTSerializable} handling and reuses
 * MBD2's {@code forge_energy} recipe capability. The MI {@code long} API delegates to that int value,
 * so the numbers are native EU (no FE↔EU conversion), only bounded to {@link Integer#MAX_VALUE}.
 */
public class MIEnergyStore extends EnergyStorage implements MIEnergyStorage, IContentChangeAware {

    private final CableTier tier;
    private Runnable onContentsChanged = () -> {};

    public MIEnergyStore(int capacity, CableTier tier) {
        super(capacity);
        this.tier = tier;
    }

    public MIEnergyStore(int capacity, int maxReceive, int maxExtract, int energy, CableTier tier) {
        super(capacity, maxReceive, maxExtract, energy);
        this.tier = tier;
    }

    // --- MIEnergyStorage (long) API, delegating to the int buffer ---

    @Override
    public long receive(long maxReceive, boolean simulate) {
        return receiveEnergy((int) Math.min(maxReceive, Integer.MAX_VALUE), simulate);
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        return extractEnergy((int) Math.min(maxExtract, Integer.MAX_VALUE), simulate);
    }

    @Override
    public long getAmount() {
        return getEnergyStored();
    }

    @Override
    public long getCapacity() {
        return getMaxEnergyStored();
    }

    @Override
    public boolean canConnect(CableTier cableTier) {
        return cableTier == this.tier;
    }

    // --- dirty tracking (fire on real change, like CopiableEnergyStorage) ---

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0) {
            onContentsChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0) {
            onContentsChanged.run();
        }
        return extracted;
    }

    /** Deep copy used by the recipe handler for simulated runs. */
    public MIEnergyStore copy() {
        return new MIEnergyStore(this.capacity, this.maxReceive, this.maxExtract, this.energy, this.tier);
    }

    public CableTier getTier() {
        return tier;
    }

    @Override
    public Runnable getOnContentsChanged() {
        return onContentsChanged;
    }

    @Override
    public void setOnContentsChanged(Runnable onContentsChanged) {
        this.onContentsChanged = onContentsChanged;
    }
}
