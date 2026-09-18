package com.roll_54.roll_mod.economy.vendingblock.integration.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.forge.REIPluginClient;

@REIPluginClient
public class GhostHandlerRegistry implements REIClientPlugin {

  @Override
  public void registerScreens(ScreenRegistry registry) {
    // Ghost-ingredient drag is handled by LdLib2's XEI integration (ItemSlot#xeiPhantom).
  }
}
