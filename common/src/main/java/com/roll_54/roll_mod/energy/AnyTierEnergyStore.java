package com.roll_54.roll_mod.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * An EU buffer that every Modern Industrialization cable tier may connect to.
 *
 * <p>{@link MIEnergyStorage} extends GrandPower's {@code ILongEnergyStorage}, which extends
 * NeoForge's {@code IEnergyStorage}, so one instance can be handed to both {@code EnergyApi.SIDED}
 * and {@code Capabilities.EnergyStorage.BLOCK}. As in {@code MIEnergyStore}, 1 FE == 1 EU with no
 * conversion; the {@code long} API delegates to the inherited {@code int} buffer, which keeps the
 * existing NBT shape and bounds the value at {@link Integer#MAX_VALUE}.
 *
 * <p>This is deliberately not {@code MIEnergyStore}: that one locks to a single {@link CableTier},
 * which is exactly what this type exists not to do. (It also implements LDLib's
 * {@code IContentChangeAware}, but that is no argument either way — {@code ldlib2} and {@code mbd2}
 * are both required dependencies.) The tier check here is open: a machine that only consumes has
 * nothing to gain from refusing an LV cable, and refusing one just reads as the block being broken.
 */
public class AnyTierEnergyStore extends EnergyStorage implements MIEnergyStorage {

    public AnyTierEnergyStore(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    /* ------------------------------ MI (long) API over the int buffer ------------------------------ */

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

    /** Every tier: lv, mv, hv, ev and superconductor all connect. */
    @Override
    public boolean canConnect(CableTier cableTier) {
        return true;
    }

    /**
     * Puts a saved amount straight into the buffer.
     *
     * <p>Loading with {@code receiveEnergy} would be clamped by {@code maxReceive}, so a machine
     * that saved a full buffer would come back holding one tick's worth of transfer. Restoring is
     * not a transfer, and nothing about it should go through the throttle.
     */
    public void setEnergy(int amount) {
        this.energy = Math.max(0, Math.min(amount, this.capacity));
    }
}
