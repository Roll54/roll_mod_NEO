package com.roll_54.roll_mod.compat.ae2.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.parts.automation.IOBusPart;
import com.roll_54.roll_mod.compat.ae2.Ae2VendorBlocking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets an AE2 import or export bus sleep while the vending block it faces blocks it.
 *
 * <p>{@code IOBusPart.tickingRequest} never sleeps on its own: a bus that did no work returns
 * {@code SLOWER} and keeps polling at its slowest rate, and on a vending block every poll walks all 54
 * storage slots through AE2's external storage facade. The method is final, so every addon bus
 * (ExtendedAE, AdvancedAE) comes through here too.
 *
 * <p>Only the no-work result is touched: when {@link Ae2VendorBlocking} says the vendor is why, the
 * bus goes to sleep, and the vendor wakes it through {@code Ae2BusWaker} on any storage, position or
 * mode change. Redstone/pulse sleep, {@code IDLE} and any other neighbour are left as AE2 has them;
 * AE2 itself wakes the bus when its config, upgrades or settings change.
 */
@Mixin(value = IOBusPart.class, remap = false)
public abstract class IOBusPartMixin {

  @Inject(method = "tickingRequest", at = @At("RETURN"), cancellable = true)
  private void roll_mod$sleepOnBlockedVendor(
      IGridNode node, int ticksSinceLastCall, CallbackInfoReturnable<TickRateModulation> cir) {
    if (cir.getReturnValue() == TickRateModulation.SLOWER && Ae2VendorBlocking.blocks((IOBusPart) (Object) this)) {
      cir.setReturnValue(TickRateModulation.SLEEP);
    }
  }
}
