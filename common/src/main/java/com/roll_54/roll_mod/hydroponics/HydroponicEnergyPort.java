package com.roll_54.roll_mod.hydroponics;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;

import java.util.function.Supplier;

/**
 * The energy face of a Hydroponic Garden Bed: an insert-only view of its node's EU buffer.
 *
 * <p>{@link MIEnergyStorage} extends GrandPower's {@code ILongEnergyStorage}, which in turn extends
 * NeoForge's {@code IEnergyStorage} with {@code int} bridges, so this one object can be handed to
 * both {@code EnergyApi.SIDED} and {@code Capabilities.EnergyStorage.BLOCK}. As in
 * {@code MIEnergyStore}, 1 FE == 1 EU with no conversion.
 *
 * <p>Transfer is unlimited and every cable tier may connect — the bed is a consumer, and throttling
 * it would only make a 25-bed field impossible to keep fed.
 */
public record HydroponicEnergyPort(Supplier<HydroponicNode> node) implements MIEnergyStorage {

    @Override
    public long receive(long maxReceive, boolean simulate) {
        HydroponicNode current = node.get();
        return current == null ? 0L : current.addEnergy(maxReceive, simulate);
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        return 0L;
    }

    @Override
    public long getAmount() {
        HydroponicNode current = node.get();
        return current == null ? 0L : current.energy();
    }

    @Override
    public long getCapacity() {
        HydroponicNode current = node.get();
        return current == null ? 0L : current.energyCapacity();
    }

    @Override
    public boolean canConnect(CableTier cableTier) {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public boolean canExtract() {
        return false;
    }
}
