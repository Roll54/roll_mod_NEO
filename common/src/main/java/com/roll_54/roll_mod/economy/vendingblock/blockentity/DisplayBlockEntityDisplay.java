package com.roll_54.roll_mod.economy.vendingblock.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.renderer.VendorBlockBaseRenderer;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.renderer.VendorBlockItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class DisplayBlockEntityDisplay implements BlockEntityRenderer<DisplayBlockEntity> {

  private final VendorBlockItemRenderer itemRenderer;
  private final VendorBlockBaseRenderer textureRenderer;

  public DisplayBlockEntityDisplay(BlockEntityRendererProvider.Context context) {
    this.itemRenderer = new VendorBlockItemRenderer();
    this.textureRenderer = new VendorBlockBaseRenderer();
  }

  public void render(
      DisplayBlockEntity entity,
      float pTick,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay) {
    ItemStack item = entity.inventory.getStackInSlot(0);
    ItemStack textureItem = entity.inventory.getStackInSlot(1);

    if (!textureItem.isEmpty() && textureItem.getItem() instanceof BlockItem blockItem) {
      textureRenderer.renderTextureOverlay(
          blockItem.getBlock(),
          poseStack,
          bufferSource,
          packedLight,
          packedOverlay,
          entity.getLevel(),
          entity.getBlockPos());
    }

    itemRenderer.renderItem(item, poseStack, bufferSource, entity.getLevel(), entity.getBlockPos());
  }
}
