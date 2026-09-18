package com.roll_54.roll_mod.economy.vendingblock.registry;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.block.VendorBlock;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegistry {
  public static final DeferredRegister.Blocks BLOCKS =
      DeferredRegister.createBlocks(RollMod.MODID);

  public static final DeferredBlock<Block> VENDOR =
      registerBlock(
          "vending_block",
          () ->
              new VendorBlock(
                  Block.Properties.of().strength(1.5f).sound(SoundType.METAL).noOcclusion()));

  public static final DeferredBlock<Block> DISPLAY =
      registerBlock(
          "display_block",
          () ->
              new com.roll_54.roll_mod.economy.vendingblock.block.DisplayBlock(
                  Block.Properties.of().strength(1.0f).sound(SoundType.METAL).noOcclusion()));

  private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
    DeferredBlock<T> toReturn = BLOCKS.register(name, block);
    registerBlockItem(name, toReturn);
    return toReturn;
  }

  private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
    ItemRegistry.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
  }

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
    BLOCKS.addAlias(ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "vending_block"), RollMod.id("vending_block"));
    BLOCKS.addAlias(ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "display_block"), RollMod.id("display_block"));
    BLOCKS.register(eventBus);
  }
}
