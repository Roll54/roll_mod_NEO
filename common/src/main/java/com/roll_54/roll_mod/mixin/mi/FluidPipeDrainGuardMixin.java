package com.roll_54.roll_mod.mixin.mi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.compat.mi.PipeTransferGuard;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Extraction side of MI fluid pipes ({@code FluidNetwork.TransferOperation.EXTRACT}). Catches exceptions
 * thrown by the neighbour's {@code drain} so a broken handler can't abort the level tick every tick.
 * See {@link PipeTransferGuard}.
 */
@Mixin(targets = "aztech.modern_industrialization.pipes.fluid.FluidNetwork$TransferOperation$2")
public abstract class FluidPipeDrainGuardMixin {

    @WrapOperation(
            method = "internalTransfer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;drain(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private FluidStack roll_mod$guardDrain(IFluidHandler handler, FluidStack resource, IFluidHandler.FluidAction action,
                                           Operation<FluidStack> original) {
        try {
            return original.call(handler, resource, action);
        } catch (RuntimeException e) {
            PipeTransferGuard.report("drain", handler, e);
            return FluidStack.EMPTY;
        }
    }
}
