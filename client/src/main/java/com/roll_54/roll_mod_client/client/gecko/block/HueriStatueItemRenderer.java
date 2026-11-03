package com.roll_54.roll_mod_client.client.gecko.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.roll_54.roll_mod.blocks.entity.HueriStatueBlockEntity;
import com.roll_54.roll_mod.registry.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the statue's item through its block entity renderer, like {@link ModuleInstallationTableItemRenderer},
 * so the item is the statue's geo model rather than its texture sheet on a cube. The item model is
 * {@code builtin/entity} and carries the display transforms; the block entity here has no level, which
 * {@link HueriStatueRenderer} takes as "an item" and draws the statue alone.
 */
public class HueriStatueItemRenderer extends BlockEntityWithoutLevelRenderer {

    private HueriStatueBlockEntity dummy;

    public HueriStatueItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (dummy == null) {
            dummy = new HueriStatueBlockEntity(BlockPos.ZERO, BlockRegistry.HUERI_STATUE.get().defaultBlockState());
        }
        Minecraft.getInstance().getBlockEntityRenderDispatcher()
                .renderItem(dummy, poseStack, buffer, packedLight, packedOverlay);
    }
}
