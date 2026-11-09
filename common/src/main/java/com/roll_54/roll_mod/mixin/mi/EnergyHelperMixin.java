package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.api.energy.EnergyApi;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.helper.EnergyHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.roll_54.roll_mod.compat.mi.perf.CapabilityCaches;
import com.roll_54.roll_mod.compat.mi.perf.EnergyOutputCacheHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Energy auto-output (generators, transformers, storage units) looked up the block in front of the machine every
 * tick. The lookup now goes through a {@link BlockCapabilityCache} kept on the machine; see {@link CapabilityCaches}.
 */
@Mixin(value = EnergyHelper.class, remap = false)
public abstract class EnergyHelperMixin {
    @WrapOperation(method = "autoOutput(Laztech/modern_industrialization/machines/MachineBlockEntity;Lnet/minecraft/core/Direction;Laztech/modern_industrialization/api/energy/CableTier;Laztech/modern_industrialization/api/energy/MIEnergyStorage;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getCapability(Lnet/neoforged/neoforge/capabilities/BlockCapability;Lnet/minecraft/core/BlockPos;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object roll_mod$cachedEnergyOutput(Level level, BlockCapability<?, ?> capability, BlockPos pos, Object context,
                                                      Operation<Object> original,
                                                      @Local(argsOnly = true) MachineBlockEntity machine) {
        if (level instanceof ServerLevel server && capability == EnergyApi.SIDED && context instanceof Direction side
                && machine instanceof EnergyOutputCacheHolder holder) {
            BlockCapabilityCache<MIEnergyStorage, Direction> cache =
                    CapabilityCaches.forTarget(holder.roll_mod$energyOutputCache(), EnergyApi.SIDED, server, pos, side);
            holder.roll_mod$setEnergyOutputCache(cache);
            return cache.getCapability();
        }
        return original.call(level, capability, pos, context);
    }
}
