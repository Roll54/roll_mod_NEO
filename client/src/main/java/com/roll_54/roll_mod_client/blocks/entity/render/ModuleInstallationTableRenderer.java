package com.roll_54.roll_mod_client.blocks.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.roll_54.roll_mod.blocks.ModuleInstallationTableBlock;
import com.roll_54.roll_mod.blocks.entity.ModuleInstallationTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import com.roll_54.roll_mod_client.client.gecko.block.ModuleInstallationTableModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * The Module Installation Table's GeckoLib geo model (static, no animations), plus the tool on it,
 * lying flat on its top face the way an item frame on the floor shows its item: same FIXED transform, same half scale, same X/Y turn a floor frame
 * gets. The stack it shows re-syncs on every module change through {@code drillChanged}, so a tool
 * that swaps its look mid-session (the saber's Meteorite module) shows the new one.
 */
public class ModuleInstallationTableRenderer extends GeoBlockRenderer<ModuleInstallationTableBlockEntity> {

    /** Half a FIXED item's thickness at half scale, plus a hair: the item lies on the surface. */
    private static final float SURFACE_OFFSET = 1.0f / 64.0f + 0.001f;

    public ModuleInstallationTableRenderer(BlockEntityRendererProvider.Context context) {
        super(new ModuleInstallationTableModel());
    }

    @Override
    public void render(ModuleInstallationTableBlockEntity table, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(table, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        ItemStack stack = table.drill();
        if (stack.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        poseStack.pushPose();
        // Block centre, just above the top face, so the item's back rests on it without z-fighting.
        poseStack.translate(0.5f, 0.98f + SURFACE_OFFSET, 0.5f);
        // Turn with the table about its centre, matching GeoBlockRenderer's own rotation for HORIZONTAL_FACING
        // (north = 0), so everything below is in the table's north-facing frame.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F
                - table.getBlockState().getValue(ModuleInstallationTableBlock.FACING).toYRot()));
        // The tool's spot on the table top and its slight skew, as laid out for a north-facing table.
        poseStack.translate(0.0f, 0.0f, -0.149f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-20.0F));
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
