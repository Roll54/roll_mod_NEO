package com.roll_54.roll_mod.economy.vendingblock.registry;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.Config;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TabRegistry {

  public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RollMod.MODID);

  public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VENDING_BLOCK_TAB =
      CREATIVE_MODE_TABS.register(
          "vending_block_tab",
          () ->
              CreativeModeTab.builder()
                  .title(Component.translatable("ui.roll_mod.creative_tab"))
                  .icon(() -> new ItemStack(BlockRegistry.VENDOR.get()))
                  .displayItems(
                      (parameters, output) -> {
                        output.accept(BlockRegistry.VENDOR.get());
                        output.accept(BlockRegistry.DISPLAY.get());
                        if (Config.Server.VENDOR_KEY_IN_CREATIVE.get())
                          output.accept(ItemRegistry.VENDOR_KEY.get());
                      })
                  .build());

  public static void register(IEventBus eventBus) {
    CREATIVE_MODE_TABS.register(eventBus);
  }
}
