package com.roll_54.roll_mod_client.client.skin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

import java.util.List;

/**
 * First-person counterpart to {@link CyberwareSkinLayer}. When the held arm has a roll_mod cyberware
 * overlay installed, cancels the vanilla arm render and redraws the arm + sleeve with the cyberware
 * texture, so the viewer's own hand matches what others see in third person.
 *
 * <p>Registered on the game bus (auto-scanned {@link EventBusSubscriber}). Overlays are opaque full
 * replacements, so — unlike CC — no base-skin underlay or glint pass is needed; a single handler
 * suffices.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class CyberwareFirstPersonArmRenderer {
    private CyberwareFirstPersonArmRenderer() {}

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (player == null || player.isInvisible()) {
            return;
        }

        PlayerCyberwareData data = PlayerCyberwareData.getForVisual(player, player.registryAccess());
        if (data == null) {
            return;
        }

        HumanoidArm arm = event.getArm();
        BodyRegion region = arm == HumanoidArm.RIGHT ? BodyRegion.RIGHT_ARM : BodyRegion.LEFT_ARM;

        CyberwareOverlay overlay = null;
        for (CyberwareOverlay candidate : CyberwareOverlays.all()) {
            if (candidate.region() == region && data.hasSpecificItem(candidate.item().get(), candidate.slot())) {
                overlay = candidate;
                break;
            }
        }
        if (overlay == null) {
            return;
        }

        EntityRenderer<? super AbstractClientPlayer> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }

        // We are taking over this arm's render.
        event.setCanceled(true);

        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        PlayerSkin.Model skinModel = player.getSkin().model();
        ResourceLocation texture = overlay.texture(skinModel);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();

        HumanoidModel.ArmPose prevRightPose = model.rightArmPose;
        HumanoidModel.ArmPose prevLeftPose = model.leftArmPose;
        boolean prevCrouching = model.crouching;
        float prevSwimAmount = model.swimAmount;
        float prevAttackTime = model.attackTime;

        List<ModelPart> parts = region.parts(model);
        boolean[] prevVisible = new boolean[parts.size()];

        poseStack.pushPose();
        try {
            model.attackTime = 0.0F;
            model.crouching = false;
            model.swimAmount = 0.0F;
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));
            for (int i = 0; i < parts.size(); i++) {
                ModelPart part = parts.get(i);
                prevVisible[i] = part.visible;
                part.visible = true;
                part.render(poseStack, vc, light, OverlayTexture.NO_OVERLAY);
            }
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();

            for (int i = 0; i < parts.size(); i++) {
                parts.get(i).visible = prevVisible[i];
            }
            model.rightArmPose = prevRightPose;
            model.leftArmPose = prevLeftPose;
            model.crouching = prevCrouching;
            model.swimAmount = prevSwimAmount;
            model.attackTime = prevAttackTime;

            poseStack.popPose();
        }
    }
}
