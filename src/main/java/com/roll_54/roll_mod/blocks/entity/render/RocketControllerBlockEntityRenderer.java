package com.roll_54.roll_mod.blocks.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.roll_54.roll_mod.blocks.entity.RocketControllerBlockEntity;
import com.roll_54.roll_mod.blocks.entity.render.model.rocket.normal.NormalRocketModel;
import com.roll_54.roll_mod.blocks.entity.render.model.rocket.small.SmallRocketModel;
import com.roll_54.roll_mod.blocks.entity.render.model.rocket.tiny.TinyRocketModel;
import com.roll_54.roll_mod.registry.TagRegistry;
import com.roll_54.roll_mod.items.spaceModule.RocketItem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class RocketControllerBlockEntityRenderer implements BlockEntityRenderer<RocketControllerBlockEntity> {

    // Textures: assets/roll_mod/textures/entity/rocket/<tier>/standard.png
    private static final ResourceLocation TEXTURE_TINY   = ResourceLocation.fromNamespaceAndPath("roll_mod", "textures/entity/rocket/tiny/standard.png");
    private static final ResourceLocation TEXTURE_SMALL  = ResourceLocation.fromNamespaceAndPath("roll_mod", "textures/entity/rocket/small/standard.png");
    private static final ResourceLocation TEXTURE_NORMAL = ResourceLocation.fromNamespaceAndPath("roll_mod", "textures/entity/rocket/normal/standard.png");

    // The raw model is 18 units wide and ~115 units tall in model-space (1 unit = 1/16 block).
    // 115 / 16 ≈ 7.1875 blocks.  We want the final visual height to match the entity sized() height.
    // uniform_scale = desired_height_blocks / 7.1875
    //   Tier 1 TINY   sized(1.1, 4.4) → 4.4 / 7.1875 ≈ 0.6122
    //   Tier 2 SMALL  sized(0.8, 3.2) → 3.2 / 7.1875 ≈ 0.4452
    //   Tier 3 NORMAL sized(1.1, 4.4) → 4.4 / 7.1875 ≈ 0.6122
    private static final float SCALE_TINY   = 0.6122f;
    private static final float SCALE_SMALL  = 0.6122f;
    private static final float SCALE_NORMAL = 0.6122f;

    private final TinyRocketModel<Entity> tinyModel;
    private final SmallRocketModel<Entity> smallModel;
    private final NormalRocketModel<Entity> normalModel;

    public RocketControllerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        tinyModel   = new TinyRocketModel<>(context.bakeLayer(TinyRocketModel.LAYER_LOCATION));
        smallModel  = new SmallRocketModel<>(context.bakeLayer(SmallRocketModel.LAYER_LOCATION));
        normalModel = new NormalRocketModel<>(context.bakeLayer(NormalRocketModel.LAYER_LOCATION));
    }

    @Override
    public void render(RocketControllerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        ItemStack rocketStack = blockEntity.getItemHandler().getStackInSlot(0);
        if (rocketStack.isEmpty() || !rocketStack.is(TagRegistry.ROCKET_ITEM)) return;

        int tier = 1;
        if (rocketStack.getItem() instanceof RocketItem ri) tier = ri.getTier();
        if (tier < 1 || tier > 3) tier = 1;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        EntityModel<Entity> model;
        ResourceLocation texture;
        float scale;

        switch (tier) {
            case 2 -> { model = smallModel;  texture = TEXTURE_SMALL;  scale = SCALE_SMALL;  }
            case 3 -> { model = normalModel; texture = TEXTURE_NORMAL; scale = SCALE_NORMAL; }
            default -> { model = tinyModel;  texture = TEXTURE_TINY;   scale = SCALE_TINY;   }
        }

        // Sample light from 1 block above the block entity so the rocket is lit
        // by the sky/ambient light rather than by the dark block interior.
        BlockPos lightSamplePos = blockEntity.getBlockPos().above(2);
        int skyLight   = level.getBrightness(LightLayer.SKY,   lightSamplePos);
        int blockLight = level.getBrightness(LightLayer.BLOCK, lightSamplePos);
        // Combine sky and block light into a packed light value; fall back to full bright if both are 0.
        int combinedLight = LightTexture.pack(blockLight, skyLight);
        if (combinedLight == 0) combinedLight = LightTexture.FULL_BRIGHT;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));

        poseStack.pushPose();

        // Center horizontally on the block, lift above block top.
        poseStack.translate(0.5, 2.2, 0.5);

        // Uniform scale: 1 model unit = 1/16 block by MC convention; negative Y flips model upright.
        poseStack.scale(scale +0.2F, -scale -0.2F, scale + 0.2F);

        // Rotate 180° so the "front" faces south (positive Z).
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));

        model.renderToBuffer(poseStack, consumer, combinedLight, packedOverlay, 0xFFFFFFFF);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(RocketControllerBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
