package com.roll_54.roll_mod.economy.vendingblock.registry;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {
  public static final DeferredRegister.Items ITEMS =
      DeferredRegister.createItems(RollMod.MODID);

  public static final DeferredItem<Item> VENDOR_KEY =
      ITEMS.register("vendor_key", () -> new Item(new Item.Properties().stacksTo(1)));

  /**
   * The economy used to be the separate mod {@code roll_mod_currency}, so everything already saved
   * on the server carries that namespace. The alias points the old name at the new one, so blocks
   * already placed in the world and items already in inventories survive the merge instead of
   * loading as air.
   *
   * <p>Safe to drop once no world predating the merge is in use.
   */
  private static final String LEGACY_NAMESPACE = "roll_mod_currency";

  public static void register(IEventBus eventBus) {
    ITEMS.addAlias(ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "vendor_key"), RollMod.id("vendor_key"));
    ITEMS.addAlias(ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "vending_block"), RollMod.id("vending_block"));
    ITEMS.addAlias(ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "display_block"), RollMod.id("display_block"));
    ITEMS.register(eventBus);
  }
}
