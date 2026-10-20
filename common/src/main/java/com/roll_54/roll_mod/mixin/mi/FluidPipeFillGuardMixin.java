package com.roll_54.roll_mod.mixin.mi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.compat.mi.PipeTransferGuard;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Insertion side of MI fluid pipes ({@code FluidNetwork.TransferOperation.INSERT}). Catches exceptions
 * thrown by the neighbour's {@code fill} so a broken handler can't abort the level tick every tick.
 * See {@link PipeTransferGuard}.
 */
@Mixin(targets = "aztech.modern_industrialization.pipes.fluid.FluidNetwork$TransferOperation$1")
public abstract class FluidPipeFillGuardMixin {

    @WrapOperation(
            method = "internalTransfer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private int roll_mod$guardFill(IFluidHandler handler, FluidStack resource, IFluidHandler.FluidAction action,
                                   Operation<Integer> original) {
        try {
            return original.call(handler, resource, action);
        } catch (RuntimeException e) {
            PipeTransferGuard.report("fill", handler, e);
            return 0;
        }
    }
}
