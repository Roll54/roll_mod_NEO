package com.roll_54.roll_mod.economy.vendingblock.registry;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockEntityRegistry {
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RollMod.MODID);

  public static final Supplier<BlockEntityType<VendorBlockEntity>> VENDOR_BE =
      BLOCK_ENTITIES.register(
          "vending_block_be",
          () ->
              BlockEntityType.Builder.of(VendorBlockEntity::new, BlockRegistry.VENDOR.get())
                  .build(null));

  public static final Supplier<
          BlockEntityType<
              com.roll_54.roll_mod.economy.vendingblock.blockentity.DisplayBlockEntity>>
      DISPLAY_BE =
          BLOCK_ENTITIES.register(
              "display_block_be",
              () ->
                  BlockEntityType.Builder.of(
                          com.roll_54.roll_mod.economy.vendingblock.blockentity.DisplayBlockEntity
                              ::new,
                          BlockRegistry.DISPLAY.get())
                      .build(null));

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
    BLOCK_ENTITIES.addAlias(ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "vending_block_be"), RollMod.id("vending_block_be"));
    BLOCK_ENTITIES.addAlias(ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "display_block_be"), RollMod.id("display_block_be"));
    BLOCK_ENTITIES.register(eventBus);
  }
}
