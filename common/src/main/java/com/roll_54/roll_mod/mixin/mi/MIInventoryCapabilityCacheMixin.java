package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.inventory.MIInventory;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.compat.mi.perf.CapabilityCaches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Machine and hatch auto-transfer: MI caches the neighbour's capability only for item auto-extract. Fluid
 * auto-extract and item/fluid auto-insert looked it up every tick. Each gets its own {@link BlockCapabilityCache}; see
 * {@link CapabilityCaches}.
 */
@Mixin(value = MIInventory.class, remap = false)
public abstract class MIInventoryCapabilityCacheMixin {
    @Unique
    private static final String GET_CAPABILITY =
            "Lnet/minecraft/world/level/Level;getCapability(Lnet/neoforged/neoforge/capabilities/BlockCapability;Lnet/minecraft/core/BlockPos;Ljava/lang/Object;)Ljava/lang/Object;";

    @Unique
    private BlockCapabilityCache<IFluidHandler, Direction> roll_mod$fluidExtractCache;
    @Unique
    private BlockCapabilityCache<IItemHandler, Direction> roll_mod$itemInsertCache;
    @Unique
    private BlockCapabilityCache<IFluidHandler, Direction> roll_mod$fluidInsertCache;

    @WrapOperation(method = "autoExtractFluids", at = @At(value = "INVOKE", target = GET_CAPABILITY))
    private Object roll_mod$cachedFluidExtract(Level level, BlockCapability<?, ?> capability, BlockPos pos, Object context,
                                               Operation<Object> original) {
        if (level instanceof ServerLevel server && capability == Capabilities.FluidHandler.BLOCK && context instanceof Direction side) {
            this.roll_mod$fluidExtractCache = CapabilityCaches.forTarget(this.roll_mod$fluidExtractCache, Capabilities.FluidHandler.BLOCK, server, pos, side);
            return this.roll_mod$fluidExtractCache.getCapability();
        }
        return original.call(level, capability, pos, context);
    }

    @WrapOperation(method = "autoInsertItems", at = @At(value = "INVOKE", target = GET_CAPABILITY))
    private Object roll_mod$cachedItemInsert(Level level, BlockCapability<?, ?> capability, BlockPos pos, Object context,
                                             Operation<Object> original) {
        if (level instanceof ServerLevel server && capability == Capabilities.ItemHandler.BLOCK && context instanceof Direction side) {
            this.roll_mod$itemInsertCache = CapabilityCaches.forTarget(this.roll_mod$itemInsertCache, Capabilities.ItemHandler.BLOCK, server, pos, side);
            return this.roll_mod$itemInsertCache.getCapability();
        }
        return original.call(level, capability, pos, context);
    }

    @WrapOperation(method = "autoInsertFluids", at = @At(value = "INVOKE", target = GET_CAPABILITY))
    private Object roll_mod$cachedFluidInsert(Level level, BlockCapability<?, ?> capability, BlockPos pos, Object context,
                                              Operation<Object> original) {
        if (level instanceof ServerLevel server && capability == Capabilities.FluidHandler.BLOCK && context instanceof Direction side) {
            this.roll_mod$fluidInsertCache = CapabilityCaches.forTarget(this.roll_mod$fluidInsertCache, Capabilities.FluidHandler.BLOCK, server, pos, side);
            return this.roll_mod$fluidInsertCache.getCapability();
        }
        return original.call(level, capability, pos, context);
    }
}
