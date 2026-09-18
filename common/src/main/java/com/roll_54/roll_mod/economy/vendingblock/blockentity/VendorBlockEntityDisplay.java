package com.roll_54.roll_mod.economy.vendingblock.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.renderer.VendorBlockBaseRenderer;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.renderer.VendorBlockItemRenderer;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.renderer.VendorBlockWarningRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class VendorBlockEntityDisplay implements BlockEntityRenderer<VendorBlockEntity> {

  private final VendorBlockItemRenderer itemRenderer;
  private final VendorBlockBaseRenderer textureRenderer;
  private final VendorBlockWarningRenderer warningRenderer;

  public VendorBlockEntityDisplay(BlockEntityRendererProvider.Context context) {
    this.itemRenderer = new VendorBlockItemRenderer();
    this.textureRenderer = new VendorBlockBaseRenderer();
    this.warningRenderer = new VendorBlockWarningRenderer();
  }

  /** Game ticks each cycled position is shown for. */
  private static final long CYCLE_TICKS = 40L;

  public void render(
      VendorBlockEntity entity,
      float pTick,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay) {
    ItemStack item = currentItem(entity);
    ItemStack textureItem = entity.getFacade();

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

    if (entity.hasError)
      warningRenderer.renderErrorCube(entity, poseStack, bufferSource, packedLight, packedOverlay);

    itemRenderer.renderItem(item, poseStack, bufferSource, entity.getLevel(), entity.getBlockPos());
  }

  /** Cycle through the positions' items over time. */
  private static ItemStack currentItem(VendorBlockEntity entity) {
    var positions = entity.getPositions();
    if (positions.isEmpty()) return ItemStack.EMPTY;
    long time = entity.getLevel() == null ? 0L : entity.getLevel().getGameTime();
    int index = (int) ((time / CYCLE_TICKS) % positions.size());
    return positions.get(index).displayStack();
  }
}
