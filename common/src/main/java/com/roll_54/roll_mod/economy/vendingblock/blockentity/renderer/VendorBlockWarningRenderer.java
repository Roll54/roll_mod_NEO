package com.roll_54.roll_mod.economy.vendingblock.blockentity.renderer;

import com.roll_54.roll_mod.RollMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class VendorBlockWarningRenderer {

  public void renderErrorCube(
      VendorBlockEntity entity,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay) {

    ResourceLocation atlasLoc = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    ResourceLocation errorTextureLoc =
        RollMod.id("block/error");

    TextureAtlasSprite errorSprite =
        Minecraft.getInstance().getModelManager().getAtlas(atlasLoc).getSprite(errorTextureLoc);
    VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.cutout());

    poseStack.pushPose();

    renderInnerCube(
        poseStack,
        vertexConsumer,
        2 / 16f,
        2 / 16f,
        2 / 16f,
        14 / 16f,
        14 / 16f,
        14 / 16f,
        errorSprite,
        packedLight,
        packedOverlay);

    poseStack.popPose();
  }

  private void renderInnerCube(
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      TextureAtlasSprite sprite,
      int packedLight,
      int packedOverlay) {

    var matrix = poseStack.last().pose();

    float textureStretch = -0.00001f;
    float uMin = sprite.getU0() - textureStretch;
    float uMax = sprite.getU1() + textureStretch;
    float vMin = sprite.getV0() - textureStretch;
    float vMax = sprite.getV1() + textureStretch;

    int red = 255;
    int green = 255;
    int blue = 255;

    // North
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y1,
        z1,
        uMax,
        vMax,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y2,
        z1,
        uMax,
        vMin,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y2,
        z1,
        uMin,
        vMin,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y1,
        z1,
        uMin,
        vMax,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);

    // South
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y1,
        z2,
        uMax,
        vMax,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y2,
        z2,
        uMax,
        vMin,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y2,
        z2,
        uMin,
        vMin,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y1,
        z2,
        uMin,
        vMax,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);

    // West
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y1,
        z2,
        uMax,
        vMax,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y2,
        z2,
        uMax,
        vMin,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y2,
        z1,
        uMin,
        vMin,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y1,
        z1,
        uMin,
        vMax,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);

    // East
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y1,
        z1,
        uMax,
        vMax,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y2,
        z1,
        uMax,
        vMin,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y2,
        z2,
        uMin,
        vMin,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y1,
        z2,
        uMin,
        vMax,
        packedLight,
        packedOverlay,
        red,
        green,
        blue);
  }

  private void addVertex(
      org.joml.Matrix4f matrix,
      VertexConsumer vertexConsumer,
      float x,
      float y,
      float z,
      float u,
      float v,
      int packedLight,
      int packedOverlay,
      int red,
      int green,
      int blue) {
    vertexConsumer
        .addVertex(matrix, x, y, z)
        .setColor(red, green, blue, 255)
        .setUv(u, v)
        .setOverlay(packedOverlay)
        .setLight(packedLight)
        .setNormal(0.0f, 1.0f, 0.0f);
  }
}
