package com.roll_54.roll_mod.economy.vendingblock.integration.jade;

import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.registry.BlockRegistry;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {

  @Override
  public void register(IWailaCommonRegistration registration) {
    registration.registerBlockDataProvider(
        VendorBlockDataProvider.INSTANCE, VendorBlockEntity.class);
  }

  @Override
  public void registerClient(IWailaClientRegistration registration) {
    registration.registerBlockComponent(
        VendorBlockComponentProvider.INSTANCE, BlockRegistry.VENDOR.get().getClass());
  }
}
