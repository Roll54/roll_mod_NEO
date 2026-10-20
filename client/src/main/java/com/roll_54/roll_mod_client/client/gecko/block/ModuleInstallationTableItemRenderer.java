package com.roll_54.roll_mod_client.client.gecko.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.roll_54.roll_mod.blocks.entity.ModuleInstallationTableBlockEntity;
import com.roll_54.roll_mod.registry.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the table's item through its block entity renderer, the way vanilla draws a chest item, so
 * the item and the placed block share the one geo model. The item model is {@code builtin/entity}
 * and carries the display transforms.
 */
public class ModuleInstallationTableItemRenderer extends BlockEntityWithoutLevelRenderer {

    private ModuleInstallationTableBlockEntity dummy;

    public ModuleInstallationTableItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (dummy == null) {
            dummy = new ModuleInstallationTableBlockEntity(BlockPos.ZERO,
                    BlockRegistry.MODULE_INSTALLATION_TABLE.get().defaultBlockState());
        }
        Minecraft.getInstance().getBlockEntityRenderDispatcher()
                .renderItem(dummy, poseStack, buffer, packedLight, packedOverlay);
    }
}
