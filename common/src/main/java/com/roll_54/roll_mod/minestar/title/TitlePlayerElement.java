package com.roll_54.roll_mod.minestar.title;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * A player standing on the title screen: skin, fixed limb pose, head and body turning after the
 * mouse, name above. Its box is the player's 0.6 × 1.8 block bounding box at the screen's scale, the
 * way vanilla's inventory doll is placed.
 *
 * <p>There is no world on the title screen, so there is no player entity to hand to vanilla's
 * {@code InventoryScreen.renderEntityInInventoryFollowsMouse}. Instead the bare {@link PlayerModel}
 * is drawn with the same transforms that call and {@code LivingEntityRenderer} would apply, and the
 * head and body angles come from the same follow-the-mouse maths.
 *
 * <p>Not given an opacity animation on purpose: an element mid-fade is drawn through LdLib's
 * off-screen layer, and a 3D model is the one thing on this screen that might not survive that. It
 * pops in after its delay instead ({@link #hideUntil}), as it did in the FancyMenu layout.
 */
@OnlyIn(Dist.CLIENT)
final class TitlePlayerElement extends UIElement {

    /** Fixed limb angles, in degrees. */
    record Pose(float leftArmX, float leftArmY, float rightArmX, float rightArmY,
                float leftLegX, float leftLegY, float rightLegX, float rightLegY) {
        static final Pose STANDING = new Pose(0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** Player height in blocks: the box is this many "blocks" tall. */
    private static final float HEIGHT_BLOCKS = 1.8f;
    /** {@code PlayerRenderer.scale}. */
    private static final float PLAYER_RENDER_SCALE = 0.9375f;

    private final Supplier<PlayerSkin> skin;
    private final String name;
    private final Pose pose;

    @Nullable private PlayerModel<?> wide;
    @Nullable private PlayerModel<?> slim;
    /** {@link Util#getMillis()} before which nothing is drawn. */
    private long hideUntil;

    TitlePlayerElement(Supplier<PlayerSkin> skin, String name, Pose pose) {
        this.skin = skin;
        this.name = name;
        this.pose = pose;
    }

    /** Keeps the player hidden for {@code millis} from now; 0 shows it at once. */
    void hideFor(long millis) {
        hideUntil = millis <= 0 ? 0 : Util.getMillis() + millis;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (Util.getMillis() < hideUntil) return;

        float x = getPositionX(), y = getPositionY(), w = getSizeWidth(), h = getSizeHeight();
        float centerX = x + w / 2f, centerY = y + h / 2f;
        // Vanilla's follow-the-mouse angles (InventoryScreen.renderEntityInInventoryFollowsMouse).
        float yaw = (float) Math.atan((centerX - context.mouseX) / 40f);
        float pitch = (float) Math.atan((centerY - context.mouseY) / 40f);

        GuiGraphics graphics = context.graphics;
        graphics.flush();
        drawModel(graphics, centerX, centerY, h / HEIGHT_BLOCKS, yaw, pitch);
        drawName(graphics, centerX, y);
    }

    private void drawModel(GuiGraphics graphics, float centerX, float centerY, float scale, float yaw, float pitch) {
        PlayerSkin current = skin.get();
        PlayerModel<?> model = model(current.model() == PlayerSkin.Model.SLIM);

        // As the doll: body turned by 20° per unit of yaw, head a further 20° and tilted by the pitch.
        float bodyYaw = 180f + yaw * 20f;
        float headYaw = yaw * 20f;
        float headPitch = -pitch * 20f;
        pose(model, headYaw, headPitch);

        PoseStack stack = graphics.pose();
        stack.pushPose();
        // InventoryScreen.renderEntityInInventory: centre of the box, flipped, entity's mid-height.
        stack.translate(centerX, centerY, 50.0);
        stack.scale(scale, scale, -scale);
        stack.translate(0f, HEIGHT_BLOCKS / 2f, 0f);
        stack.mulPose(new Quaternionf().rotateZ(Mth.PI).mul(new Quaternionf().rotateX(pitch * 20f * Mth.DEG_TO_RAD)));
        // LivingEntityRenderer.render / setupRotations, PlayerRenderer.scale.
        stack.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        stack.scale(-1f, -1f, 1f);
        stack.scale(PLAYER_RENDER_SCALE, PLAYER_RENDER_SCALE, PLAYER_RENDER_SCALE);
        stack.translate(0f, -1.501f, 0f);

        Lighting.setupForEntityInInventory();
        model.renderToBuffer(stack, graphics.bufferSource().getBuffer(RenderType.entityTranslucent(current.texture())),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
        graphics.flush();
        stack.popPose();
        Lighting.setupFor3DItems();
    }

    /** The name over the head, on a faint plate like a name tag. */
    private void drawName(GuiGraphics graphics, float centerX, float top) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(name);
        int x = Math.round(centerX - width / 2f);
        int y = Math.round(top) - font.lineHeight - 3;
        graphics.fill(x - 1, y - 1, x + width + 1, y + font.lineHeight, 0x40000000);
        graphics.drawString(font, name, x, y, 0xFFFFFFFF, false);
        graphics.flush();
    }

    /** Sets every part's angles: no {@code setupAnim} without an entity, so the outer layers are copied by hand. */
    private void pose(PlayerModel<?> model, float headYaw, float headPitch) {
        set(model.head, headPitch, headYaw);
        set(model.body, 0, 0);
        set(model.leftArm, pose.leftArmX(), pose.leftArmY());
        set(model.rightArm, pose.rightArmX(), pose.rightArmY());
        set(model.leftLeg, pose.leftLegX(), pose.leftLegY());
        set(model.rightLeg, pose.rightLegX(), pose.rightLegY());
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
    }

    private static void set(ModelPart part, float xDegrees, float yDegrees) {
        part.xRot = xDegrees * Mth.DEG_TO_RAD;
        part.yRot = yDegrees * Mth.DEG_TO_RAD;
        part.zRot = 0f;
    }

    /** Baked on first use, per arm width: the skin's model type is only known once it has loaded. */
    private PlayerModel<?> model(boolean slimArms) {
        if (slimArms) {
            if (slim == null) slim = bake(ModelLayers.PLAYER_SLIM, true);
            return slim;
        }
        if (wide == null) wide = bake(ModelLayers.PLAYER, false);
        return wide;
    }

    private static PlayerModel<?> bake(net.minecraft.client.model.geom.ModelLayerLocation layer, boolean slimArms) {
        PlayerModel<?> model = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(layer), slimArms);
        // A model starts out as a baby's (big head, half-size body) until a renderer says otherwise
        // each frame from the entity; there is no entity here, so say it once.
        model.young = false;
        return model;
    }
}
