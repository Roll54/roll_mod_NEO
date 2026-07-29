package com.roll_54.roll_mod_client.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Draws roll_mod cyberware textures onto the player. Reads createcybernetics' synced
 * {@link PlayerCyberwareData} (the same data source CC's own renderer uses) and, for every installed
 * item registered in {@link CyberwareOverlays}, re-renders that region's model parts with the
 * cyberware texture. Third-person only.
 *
 * <p>The overlay draws after the base model, so an opaque arm texture covers the flesh. Parts are
 * force-made-visible around each draw because {@link ModelPart#render} early-returns on an invisible
 * part, and CC's {@code CyberwareLimbHider} may have toggled the base limb off this frame.
 *
 * <p>A limb texture is a full player-skin layout, so it can also paint the torso UV (e.g. an arm's
 * shoulder/chest connection). Those pixels live on {@code model.body}, which is not part of the limb
 * region, so each region additionally reports {@link BodyRegion#bodyOverlayParts} that are drawn here
 * too. First-person rendering deliberately ignores them (the torso is off-screen there).
 */
public class CyberwareSkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public CyberwareSkinLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }

        PlayerCyberwareData data = PlayerCyberwareData.getForVisual(player, player.registryAccess());
        if (data == null) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = getParentModel();
        PlayerSkin.Model skinModel = player.getSkin().model();

        for (CyberwareOverlay overlay : CyberwareOverlays.all()) {
            if (!data.hasSpecificItem(overlay.item().get(), overlay.slot())) {
                continue;
            }

            ResourceLocation texture = overlay.texture(skinModel);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));

            drawParts(overlay.region().parts(model), poseStack, vc, packedLight);
            // Also paint the torso pixels the limb texture carries (e.g. shoulder/chest).
            drawParts(overlay.region().bodyOverlayParts(model), poseStack, vc, packedLight);
        }
    }

    private static void drawParts(List<ModelPart> parts, PoseStack poseStack, VertexConsumer vc, int packedLight) {
        for (ModelPart part : parts) {
            boolean wasVisible = part.visible;
            part.visible = true;
            part.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            part.visible = wasVisible;
        }
    }
}
