package com.roll_54.roll_mod.economy.vendingblock.integration.jei;

import com.roll_54.roll_mod.RollMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class GhostHandlerRegistry implements IModPlugin {

  @Override
  public ResourceLocation getPluginUid() {
    // Not "jei_plugin": GTMOGS, bundled and namespaced roll_mod, takes roll_mod:jei_plugin.
    return RollMod.id("vending_ghost_jei_plugin");
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    // Ghost-ingredient drag is handled by LdLib2's XEI integration (ItemSlot#xeiPhantom).
  }
}
