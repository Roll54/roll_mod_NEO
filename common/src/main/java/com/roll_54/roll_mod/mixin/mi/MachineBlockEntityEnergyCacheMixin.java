package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import com.roll_54.roll_mod.compat.mi.perf.EnergyOutputCacheHolder;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Storage for the cached energy output target; see {@link EnergyHelperMixin}. */
@Mixin(value = MachineBlockEntity.class, remap = false)
public abstract class MachineBlockEntityEnergyCacheMixin implements EnergyOutputCacheHolder {
    @Unique
    private BlockCapabilityCache<MIEnergyStorage, Direction> roll_mod$energyOutputCache;

    @Override
    public BlockCapabilityCache<MIEnergyStorage, Direction> roll_mod$energyOutputCache() {
        return this.roll_mod$energyOutputCache;
    }

    @Override
    public void roll_mod$setEnergyOutputCache(BlockCapabilityCache<MIEnergyStorage, Direction> cache) {
        this.roll_mod$energyOutputCache = cache;
    }
}
