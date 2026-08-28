package com.roll_54.roll_mod_client.client.gecko.block;

import com.roll_54.roll_mod.blocks.entity.HueriStatueBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HueriStatueRenderer extends GeoBlockRenderer<HueriStatueBlockEntity> {
    public HueriStatueRenderer(BlockEntityRendererProvider.Context context) {
        super(new HueriStatueModel());
    }

    /**
     * The statue is two blocks tall, so the default unit-cube scope would cull it
     * as soon as the block itself leaves the frustum.
     */
    @Override
    public AABB getRenderBoundingBox(HueriStatueBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0, 1, 0);
    }
}
