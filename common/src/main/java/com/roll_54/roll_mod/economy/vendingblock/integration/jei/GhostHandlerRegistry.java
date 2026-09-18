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
    return RollMod.id("jei_plugin");
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    // Ghost-ingredient drag is handled by LdLib2's XEI integration (ItemSlot#xeiPhantom).
  }
}
