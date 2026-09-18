package com.roll_54.roll_mod.economy.vendingblock.blockentity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class VendorBlockBaseRenderer {

  private static final float TEXTURE_STRETCH = -0.0005f;

  public void renderTextureOverlay(
      Block block,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay,
      Level level,
      BlockPos pos) {
    ResourceLocation atlasLoc = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    var mc = Minecraft.getInstance();
    var state = block.defaultBlockState();
    var blockRenderer = mc.getBlockRenderer();
    var model = blockRenderer.getBlockModel(state);
    VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.cutout());
    poseStack.pushPose();
    int tintColor = getBiomeColor(block, level, pos);

    TextureAtlasSprite faceUp = getBlockTexture(model, state, Direction.UP, mc, atlasLoc);
    TextureAtlasSprite faceDown = getBlockTexture(model, state, Direction.DOWN, mc, atlasLoc);
    TextureAtlasSprite faceNorth = getBlockTexture(model, state, Direction.NORTH, mc, atlasLoc);
    TextureAtlasSprite faceSouth = getBlockTexture(model, state, Direction.SOUTH, mc, atlasLoc);
    TextureAtlasSprite faceWest = getBlockTexture(model, state, Direction.WEST, mc, atlasLoc);
    TextureAtlasSprite faceEast = getBlockTexture(model, state, Direction.EAST, mc, atlasLoc);

    renderCube(
        poseStack,
        vertexConsumer,
        TEXTURE_STRETCH,
        TEXTURE_STRETCH,
        TEXTURE_STRETCH,
        1f - TEXTURE_STRETCH,
        0.125f - TEXTURE_STRETCH,
        1f - TEXTURE_STRETCH,
        faceUp,
        faceDown,
        faceNorth,
        faceSouth,
        faceWest,
        faceEast,
        packedLight,
        packedOverlay,
        tintColor);
    poseStack.popPose();
  }

  private void renderCube(
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      TextureAtlasSprite faceUp,
      TextureAtlasSprite faceDown,
      TextureAtlasSprite faceNorth,
      TextureAtlasSprite faceSouth,
      TextureAtlasSprite faceWest,
      TextureAtlasSprite faceEast,
      int packedLight,
      int packedOverlay,
      int tintColor) {

    var matrix = poseStack.last().pose();
    int red = (tintColor >> 16) & 0xFF;
    int green = (tintColor >> 8) & 0xFF;
    int blue = tintColor & 0xFF;

    // Bottom
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y1,
        z1,
        faceDown.getU0(),
        faceDown.getV0(),
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
        faceDown.getU1(),
        faceDown.getV0(),
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
        faceDown.getU1(),
        faceDown.getV1(),
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
        faceDown.getU0(),
        faceDown.getV1(),
        packedLight,
        packedOverlay,
        red,
        green,
        blue);

    // Top
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y2,
        z2,
        faceUp.getU0(),
        faceUp.getV1(),
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
        faceUp.getU1(),
        faceUp.getV1(),
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
        faceUp.getU1(),
        faceUp.getV0(),
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
        faceUp.getU0(),
        faceUp.getV0(),
        packedLight,
        packedOverlay,
        red,
        green,
        blue);

    // North
    float v0 = faceNorth.getV1() * (1f - 2 / 16f) + faceNorth.getV0() * (2 / 16f);
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y1,
        z1,
        faceNorth.getU1(),
        faceNorth.getV1(),
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
        faceNorth.getU1(),
        v0,
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
        faceNorth.getU0(),
        v0,
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
        faceNorth.getU0(),
        faceNorth.getV1(),
        packedLight,
        packedOverlay,
        red,
        green,
        blue);

    // South
    v0 = faceSouth.getV1() * (1 - 2 / 16f) + faceSouth.getV0() * (2 / 16f);
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y1,
        z2,
        faceSouth.getU1(),
        faceSouth.getV1(),
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
        faceSouth.getU1(),
        v0,
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
        faceSouth.getU0(),
        v0,
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
        faceSouth.getU0(),
        faceSouth.getV1(),
        packedLight,
        packedOverlay,
        red,
        green,
        blue);

    // West
    v0 = faceWest.getV1() * (1f - 2 / 16f) + faceWest.getV0() * (2 / 16f);
    addVertex(
        matrix,
        vertexConsumer,
        x1,
        y1,
        z2,
        faceWest.getU1(),
        faceWest.getV1(),
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
        faceWest.getU1(),
        v0,
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
        faceWest.getU0(),
        v0,
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
        faceWest.getU0(),
        faceWest.getV1(),
        packedLight,
        packedOverlay,
        red,
        green,
        blue);

    // East
    v0 = faceEast.getV1() * (1f - 2 / 16f) + faceEast.getV0() * (2 / 16f);
    addVertex(
        matrix,
        vertexConsumer,
        x2,
        y1,
        z1,
        faceEast.getU1(),
        faceEast.getV1(),
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
        faceEast.getU1(),
        v0,
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
        faceEast.getU0(),
        v0,
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
        faceEast.getU0(),
        faceEast.getV1(),
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

  private TextureAtlasSprite getBlockTexture(
      BakedModel model, BlockState state, Direction face, Minecraft mc, ResourceLocation atlasLoc) {
    var quads = model.getQuads(state, face, mc.level != null ? mc.level.random : null);
    if (quads != null && !quads.isEmpty()) {
      return quads.get(0).getSprite();
    }
    return model.getParticleIcon();
  }

  private int getBiomeColor(Block block, Level level, BlockPos pos) {

    if (level == null) return 0xFFFFFF;
    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
    String blockPath = blockId.getPath();

    if (blockPath.contains("grass_block") || blockPath.contains("grass")) {
      return Minecraft.getInstance()
          .getBlockColors()
          .getColor(block.defaultBlockState(), level, pos, 0);
    } else if (blockPath.contains("leaves")) {
      return Minecraft.getInstance()
          .getBlockColors()
          .getColor(block.defaultBlockState(), level, pos, 0);
    }

    return 0xFFFFFF;
  }
}
