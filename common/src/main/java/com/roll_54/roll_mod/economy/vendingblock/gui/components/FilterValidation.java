package com.roll_54.roll_mod.economy.vendingblock.gui.components;

import com.roll_54.roll_mod.economy.vendingblock.Config;
import com.roll_54.roll_mod.economy.vendingblock.gui.chat.Messages;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Reusable validation for vendor/display filter slots, extracted from {@link FilterSlot} so the
 * LdLib2 modular UIs can validate items before sending a {@link
 * com.roll_54.roll_mod.economy.vendingblock.network.FilterSlotUpdatePacket}.
 *
 * <p>The {@code level}/{@code pos} arguments locate the block so server-side messaging and shape
 * lookups behave exactly as the old slot did. Messaging only fires on the server side.
 */
public final class FilterValidation {

  private FilterValidation() {}

  /** True when the item is a full-cube block, mirroring the old facade restriction. */
  public static boolean isFullBlock(ItemStack stack, Level level, BlockPos pos) {
    if (!(stack.getItem() instanceof BlockItem blockItem)) return false;

    Block block = blockItem.getBlock();
    BlockState state = block.defaultBlockState();

    VoxelShape shape;
    try {
      if (level != null && pos != null) {
        shape = state.getShape(level, pos);
      } else {
        shape = state.getShape(null, null);
      }
    } catch (Exception e) {
      return false;
    }

    if (!shape.equals(Shapes.block())) {
      if (level != null && pos != null && !level.isClientSide()) {
        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 100, false);
        if (player != null) {
          player.sendSystemMessage(Messages.fullBlockFacade(stack.getHoverName().getString()));
        }
      }
      return false;
    }
    return true;
  }

  public static boolean isBlacklistedProduct(ItemStack stack, Level level, BlockPos pos) {
    return isBlacklisted(stack, level, pos, Config.Server.PRODUCT_BLACKLIST.get());
  }

  public static boolean isBlacklistedFacade(ItemStack stack, Level level, BlockPos pos) {
    return isBlacklisted(stack, level, pos, Config.Server.FACADE_BLACKLIST.get());
  }

  private static boolean isBlacklisted(
      ItemStack stack, Level level, BlockPos pos, List<? extends String> blacklist) {
    Item item = stack.getItem();
    String itemId = item.toString();
    boolean match = false;

    for (String entry : blacklist) {
      if (entry.equals(itemId) || ("minecraft:" + entry).equals(itemId)) {
        match = true;
      } else if (entry.contains("*")) {
        match = wildcardMatch(entry, itemId) || wildcardMatch("minecraft:" + entry, itemId);
      } else if (entry.startsWith("#")) {
        match = isItemInTag(entry.substring(1), stack);
      }
      if (match) break;
    }

    if (match && level != null && pos != null && !level.isClientSide()) {
      Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 100, false);
      if (player != null) {
        player.sendSystemMessage(Messages.blacklistedFacade(stack.getHoverName().getString()));
      }
    }

    return match;
  }

  private static boolean wildcardMatch(String pattern, String str) {
    String regex = pattern.replace("*", ".*");
    return str.matches(regex);
  }

  @SuppressWarnings("deprecation")
  public static boolean isItemInTag(String tagString, ItemStack stack) {
    ResourceLocation tagId = ResourceLocation.tryParse(tagString);
    if (tagId == null) {
      return false;
    }

    TagKey<Item> itemTagKey = TagKey.create(Registries.ITEM, tagId);
    TagKey<Block> blockTagKey = TagKey.create(Registries.BLOCK, tagId);

    Level level = null;
    if (ServerLifecycleHooks.getCurrentServer() != null) {
      level = ServerLifecycleHooks.getCurrentServer().overworld();
    }

    if (level != null) {
      RegistryAccess registryAccess = level.registryAccess();
      var itemRegistry = registryAccess.registryOrThrow(Registries.ITEM);
      var itemTag = itemRegistry.getTag(itemTagKey);
      if (itemTag != null) {
        if (stack.getItem().builtInRegistryHolder().is(itemTagKey)) return true;
      }

      if (stack.getItem() instanceof BlockItem blockItem) {
        var blockRegistry = registryAccess.registryOrThrow(Registries.BLOCK);
        var blockTag = blockRegistry.getTag(blockTagKey);
        if (blockTag != null) {
          if (blockItem.getBlock().builtInRegistryHolder().is(blockTagKey)) return true;
        }
      }
    }
    return false;
  }
}
