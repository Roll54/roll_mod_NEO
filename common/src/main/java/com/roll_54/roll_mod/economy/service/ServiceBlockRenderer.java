package com.roll_54.roll_mod.economy.service;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The service's icon on the block's front ({@link ServiceType#blockIconTexture()}): a 5×5 pixel square
 * centred on the plinth that sits on top of the lower half — the strip of the upper model between the
 * lower block's top edge and the slanted console. Nothing is drawn until a service is picked.
 */
public class ServiceBlockRenderer implements BlockEntityRenderer<ServiceBlockEntity> {

  /** Model pixels, in the north-facing model: x 5.5–10.5, y 16.5–21.5, just in front of z = 0. */
  private static final float X0 = 5.5F / 16, X1 = 10.5F / 16;
  private static final float Y0 = 16.5F / 16, Y1 = 21.5F / 16;
  private static final float Z = -0.002F;

  public ServiceBlockRenderer(BlockEntityRendererProvider.Context context) {}

  @Override
  public void render(ServiceBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                     int packedLight, int packedOverlay) {
    ServiceType type = be.getServiceType();
    Level level = be.getLevel();
    BlockState state = be.getBlockState();
    if (type == null || level == null || !state.hasProperty(ServiceBlock.FACING)) return;
    Direction facing = state.getValue(ServiceBlock.FACING);

    // Lit like the face it sits on: the open space in front of the upper half.
    int light = LevelRenderer.getLightColor(level, be.getBlockPos().above().relative(facing));

    poseStack.pushPose();
    // Same turn as the blockstate's y rotation: north is the model's own front.
    poseStack.translate(0.5, 0, 0.5);
    poseStack.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
    poseStack.translate(-0.5, 0, -0.5);

    PoseStack.Pose pose = poseStack.last();
    VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(type.blockIconTexture()));
    // Seen from the north, +x is on the viewer's left, so u runs from X1 to X0.
    vertex(vc, pose, X1, Y1, 0, 0, light);
    vertex(vc, pose, X1, Y0, 0, 1, light);
    vertex(vc, pose, X0, Y0, 1, 1, light);
    vertex(vc, pose, X0, Y1, 1, 0, light);
    poseStack.popPose();
  }

  private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
    vc.addVertex(pose, x, y, Z)
        .setColor(255, 255, 255, 255)
        .setUv(u, v)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(pose, 0, 0, -1);
  }

  /** Both halves, so the icon on the upper one is not culled with the lower block out of view. */
  @Override
  public AABB getRenderBoundingBox(ServiceBlockEntity be) {
    BlockPos pos = be.getBlockPos();
    return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
  }
}
