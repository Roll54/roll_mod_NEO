package com.roll_54.roll_mod.economy;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.DisplayBlockEntityDisplay;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntityDisplay;
import com.roll_54.roll_mod.economy.vendingblock.gui.hud.HintOverlay;
import com.roll_54.roll_mod.economy.vendingblock.registry.BlockEntityRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-side setup for the economy: the vendor block renderers and the crosshair hint overlay. */
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class EconomyClientEvents {

  private EconomyClientEvents() {}

  @SubscribeEvent
  public static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> NeoForge.EVENT_BUS.register(HintOverlay.class));
  }

  @SubscribeEvent
  public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(
        BlockEntityRegistry.VENDOR_BE.get(), VendorBlockEntityDisplay::new);
    event.registerBlockEntityRenderer(
        BlockEntityRegistry.DISPLAY_BE.get(), DisplayBlockEntityDisplay::new);
  }
}
