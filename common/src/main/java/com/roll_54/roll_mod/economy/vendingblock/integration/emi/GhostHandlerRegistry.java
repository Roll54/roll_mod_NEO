package com.roll_54.roll_mod.economy.vendingblock.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class GhostHandlerRegistry implements EmiPlugin {

  @Override
  public void register(EmiRegistry registry) {
    // Ghost-ingredient drag is handled by LdLib2's XEI integration (ItemSlot#xeiPhantom).
  }
}
