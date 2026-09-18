package com.roll_54.roll_mod.economy.vendingblock.blockentity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.roll_54.roll_mod.economy.vendingblock.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class VendorBlockItemRenderer {

  private float rotation;
  private long lastTime = System.currentTimeMillis();

  public void renderItem(
      ItemStack item,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      Level level,
      BlockPos pos) {
    ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

    poseStack.pushPose();
    poseStack.translate(0.5f, 0.5f + getBobbingOffset(), 0.5f);
    poseStack.scale(0.5f, 0.5f, 0.5f);
    poseStack.mulPose(Axis.YP.rotationDegrees(getRotation(level, pos)));

    int lightLevel = getLightLevel(level, pos);
    itemRenderer.renderStatic(
        item,
        ItemDisplayContext.FIXED,
        lightLevel,
        OverlayTexture.NO_OVERLAY,
        poseStack,
        bufferSource,
        level,
        1);

    poseStack.popPose();
  }

  private float getRotation(Level level, BlockPos pos) {
    if (Config.Client.ANIMATION_MODE.get() == Config.Client.AnimationMode.ROTATION
        || Config.Client.ANIMATION_MODE.get() == Config.Client.AnimationMode.BOBBING_ROTATION
        || Config.Client.ANIMATION_MODE.get() == Config.Client.AnimationMode.SERVER_DEFAULT
            && (Config.Server.ANIMATION_MODE.get() == Config.Server.AnimationMode.ROTATION
                || Config.Server.ANIMATION_MODE.get()
                    == Config.Server.AnimationMode.BOBBING_ROTATION)
        || (level == null
            || level.getNearestPlayer(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, false)
                == null)) {
      long currentTime = System.currentTimeMillis();
      long elapsedTime = currentTime - lastTime;
      lastTime = currentTime;

      rotation += (elapsedTime / 16.0f);
      if (rotation >= 360.0f) rotation -= 360.0f;
      return rotation;
    } else if (Config.Client.ANIMATION_MODE.get() == Config.Client.AnimationMode.FACING_PLAYER
        || Config.Client.ANIMATION_MODE.get() == Config.Client.AnimationMode.SERVER_DEFAULT
            && Config.Server.ANIMATION_MODE.get() == Config.Server.AnimationMode.FACING_PLAYER) {
      double blockX = pos.getX() + 0.5;
      double blockZ = pos.getZ() + 0.5;
      double playerX = level.getNearestPlayer(blockX, pos.getY() + 0.5, blockZ, 10, false).getX();
      double playerZ = level.getNearestPlayer(blockX, pos.getY() + 0.5, blockZ, 10, false).getZ();

      double angle = Math.atan2(playerZ - blockZ, playerX - blockX);
      return -(float) Math.toDegrees(angle) - 90.0f;
    } else {
      return 0.0f;
    }
  }

  private float getBobbingOffset() {
    long currentTime = System.currentTimeMillis();
    if (Config.Client.ANIMATION_MODE.get() == Config.Client.AnimationMode.BOBBING
        || Config.Client.ANIMATION_MODE.get() == Config.Client.AnimationMode.BOBBING_ROTATION
        || Config.Client.ANIMATION_MODE.get() == Config.Client.AnimationMode.SERVER_DEFAULT
            && (Config.Server.ANIMATION_MODE.get() == Config.Server.AnimationMode.BOBBING
                || Config.Server.ANIMATION_MODE.get()
                    == Config.Server.AnimationMode.BOBBING_ROTATION)) {
      return (float) Math.sin(currentTime / 500.0) * 0.1f;
    } else {
      return 0.0f;
    }
  }

  private int getLightLevel(Level level, BlockPos pos) {
    int bLight = level.getBrightness(LightLayer.BLOCK, pos);
    int sLight = level.getBrightness(LightLayer.SKY, pos);
    return LightTexture.pack(bLight, sLight);
  }
}
