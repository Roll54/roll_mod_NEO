package com.roll_54.roll_mod_client.client.gecko;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.roll_54.roll_mod.items.armor.geckolib.HazmatHelmetItem;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HazmatHelmetRenderer extends GeoArmorRenderer<HazmatHelmetItem> {

    /**
     * The mask's shell is modelled about 8.7px across, inside the skin's hat layer (9px): a skin's hat or hair poked
     * through it. Scaled about the head's centre (the {@code hazmatShell} bone's pivot) to a vanilla helmet's 10px.
     */
    private static final float SHELL_SCALE = 1.15f;

    public HazmatHelmetRenderer() {
        super(new HazmatHelmetModel());
    }

    @Override
    public void preRender(PoseStack poseStack, HazmatHelmetItem animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        model.getBone("hazmatShell").ifPresent(bone -> bone.updateScale(SHELL_SCALE, SHELL_SCALE, SHELL_SCALE));
    }
}
