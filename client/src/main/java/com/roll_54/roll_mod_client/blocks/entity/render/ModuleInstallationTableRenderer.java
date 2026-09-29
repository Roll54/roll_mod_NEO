package com.roll_54.roll_mod_client.blocks.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.roll_54.roll_mod.blocks.entity.ModuleInstallationTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * The tool on the Module Installation Table, lying flat on its top face the way an item frame on
 * the floor shows its item: same FIXED transform, same half scale, same X/Y turn a floor frame
 * gets. The stack it shows re-syncs on every module change through {@code drillChanged}, so a tool
 * that swaps its look mid-session (the saber's Meteorite module) shows the new one.
 */
public class ModuleInstallationTableRenderer implements BlockEntityRenderer<ModuleInstallationTableBlockEntity> {

    /** Half a FIXED item's thickness at half scale, plus a hair: the item lies on the surface. */
    private static final float SURFACE_OFFSET = 1.0f / 64.0f + 0.001f;

    public ModuleInstallationTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ModuleInstallationTableBlockEntity table, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = table.drill();
        if (stack.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        poseStack.pushPose();
        // Just above the top face, so the item's back rests on it without z-fighting.
        poseStack.translate(0.5f, 1.0f + SURFACE_OFFSET, 0.5f);
        // What ItemFrameRenderer applies to a frame facing up (xRot -90, yRot 0).
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.5f, 0.5f, 0.5f);

        // Lit by the air above the table: the table itself is opaque, so its own light is zero.
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED,
                getLightLevel(table.getLevel(), table.getBlockPos().above()), OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, table.getLevel(), 1);
        poseStack.popPose();
    }

    private int getLightLevel(Level level, BlockPos pos) {
        int bLight = level.getBrightness(LightLayer.BLOCK, pos);
        int sLight = level.getBrightness(LightLayer.SKY, pos);
        return LightTexture.pack(bLight, sLight);
    }
}
