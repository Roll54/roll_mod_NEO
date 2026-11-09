package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.pipes.api.PipeNetwork;
import com.roll_54.roll_mod.compat.mi.energy.PipeNetworkTickingGeneration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Counts rebuilds of MI's ticking-node list, so energy networks know when to recollect their endpoints
 * ({@link com.roll_54.roll_mod.compat.mi.energy.DirectEnergyTransfer}).
 */
@Mixin(value = PipeNetwork.class, remap = false)
public abstract class PipeNetworkMixin implements PipeNetworkTickingGeneration {
    @Shadow
    boolean tickingCacheValid;

    @Unique
    private int roll_mod$tickingGeneration;

    @Inject(method = "iterateTickingNodes", at = @At("HEAD"))
    private void roll_mod$countRebuild(CallbackInfoReturnable<Collection<PipeNetwork.PosNode>> cir) {
        if (!this.tickingCacheValid) {
            this.roll_mod$tickingGeneration++;
        }
    }

    @Override
    public int roll_mod$tickingGeneration() {
        return this.roll_mod$tickingGeneration;
    }
}
