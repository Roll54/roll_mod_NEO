package com.roll_54.roll_mod_client.client.gecko.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.roll_54.roll_mod.blocks.ClientHueriStatue;
import com.roll_54.roll_mod.blocks.entity.HueriStatueBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HueriStatueRenderer extends GeoBlockRenderer<HueriStatueBlockEntity> {

    /** Half a block clear of the statue's head, which tops out just under 2 blocks. */
    private static final double MARK_Y = 2.95;
    private static final float MARK_SCALE = 0.05F;
    private static final Component MARK = Component.literal("!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);

    /** The donated flowers' holograms: green, see-through, one block below the statue's base. */
    private static final float TINT_R = 0.45F;
    private static final float TINT_G = 1.0F;
    private static final float TINT_B = 0.55F;
    private static final int HOLO_ALPHA = 150;
    private static final double FLOWER_Y = -1.0;
    private static final float NAME_SCALE = 0.02F;
    private static final int NAME_COLOR = 0xFF76CCA8;
    /** A donor's name shows within this of the camera, or further while looked at. */
    private static final double NAME_NEAR = 3.0;
    private static final double NAME_LOOK = 16.0;
    private static final double NAME_LOOK_DOT = 0.995;
    /** Covers the flowers' disc ({@code HueriStatueFlowers}' 7.5 blocks) with room to spare. */
    private static final double REACH = 8.0;

    private final Font font;

    public HueriStatueRenderer(BlockEntityRendererProvider.Context context) {
        super(new HueriStatueModel());
        this.font = context.getFont();
    }

    /**
     * The statue is two blocks tall, so the default unit-cube scope would cull it
     * as soon as the block itself leaves the frustum. The extra block is the "!"; with flowers
     * donated it spans their disc too.
     */
    @Override
    public AABB getRenderBoundingBox(HueriStatueBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        if (blockEntity.flowers().isEmpty()) return new AABB(pos).expandTowards(0, 3, 0);
        return new AABB(pos.getX() - REACH, pos.getY() - 1, pos.getZ() - REACH,
                pos.getX() + 1 + REACH, pos.getY() + 3, pos.getZ() + 1 + REACH);
    }

    @Override
    public void render(HueriStatueBlockEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        if (!ClientHueriStatue.seen) renderMark(animatable, partialTick, poseStack, bufferSource);
        if (!animatable.flowers().isEmpty()) renderFlowers(animatable, partialTick, poseStack, bufferSource);
    }

    /**
     * Each donated flower as its block model, tinted green, at {@link #HOLO_ALPHA} and full
     * brightness, bobbing gently, with its donor's name over it while near or looked at.
     */
    private void renderFlowers(HueriStatueBlockEntity animatable, float partialTick, PoseStack poseStack,
                               MultiBufferSource bufferSource) {
        float time = (animatable.getLevel() == null ? 0L : animatable.getLevel().getGameTime()) + partialTick;
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        Vector3f look = camera.getLookVector();
        BlockPos pos = animatable.getBlockPos();
        VertexConsumer holo = new HoloConsumer(bufferSource.getBuffer(Sheets.translucentCullBlockSheet()));

        for (int i = 0; i < animatable.flowers().size(); i++) {
            HueriStatueBlockEntity.Flower flower = animatable.flowers().get(i);
            Item item = BuiltInRegistries.ITEM.get(flower.item());
            if (!(item instanceof BlockItem blockItem)) continue;
            BlockState state = blockItem.getBlock().defaultBlockState();
            double bob = Mth.sin(time * 0.05F + i * 1.7F) * 0.05;

            poseStack.pushPose();
            poseStack.translate(0.5 + flower.dx(), FLOWER_Y + bob, 0.5 + flower.dz());
            poseStack.mulPose(Axis.YP.rotationDegrees(flower.yaw()));
            poseStack.translate(-0.5, 0, -0.5);
            blocks.getModelRenderer().renderModel(poseStack.last(), holo, state, blocks.getBlockModel(state),
                    1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();

            Vec3 at = new Vec3(pos.getX() + 0.5 + flower.dx(), pos.getY() + FLOWER_Y + 0.5, pos.getZ() + 0.5 + flower.dz());
            if (lookedAt(eye, look, at)) renderName(flower.donorName(), flower.dx(), FLOWER_Y + 1.15 + bob, flower.dz(),
                    poseStack, bufferSource);
        }
    }

    private static boolean lookedAt(Vec3 eye, Vector3f look, Vec3 at) {
        Vec3 to = at.subtract(eye);
        double distance = to.length();
        if (distance < NAME_NEAR) return true;
        if (distance > NAME_LOOK) return false;
        return (to.x * look.x() + to.y * look.y() + to.z * look.z()) / distance > NAME_LOOK_DOT;
    }

    private void renderName(String name, double dx, double y, double dz, PoseStack poseStack,
                            MultiBufferSource bufferSource) {
        poseStack.pushPose();
        poseStack.translate(0.5 + dx, y, 0.5 + dz);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(NAME_SCALE, -NAME_SCALE, NAME_SCALE);
        font.drawInBatch(name, -font.width(name) / 2.0F, 0, NAME_COLOR, true, poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    /**
     * Passes everything through but the colour, which it tints green and makes see-through — what
     * makes the flowers holograms. The tint is applied here because {@code renderModel}'s own colour
     * only reaches quads with a tint index, and a flower's petals have none.
     */
    private record HoloConsumer(VertexConsumer inner) implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int ignored) {
            inner.setColor((int) (red * TINT_R), (int) (green * TINT_G), (int) (blue * TINT_B), HOLO_ALPHA);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            inner.setNormal(x, y, z);
            return this;
        }
    }

    /**
     * A bobbing "!" facing the camera, the way a name tag does, at full brightness — the statue's
     * "you have not read me yet", gone once this player opens any statue.
     */
    private void renderMark(HueriStatueBlockEntity animatable, float partialTick, PoseStack poseStack,
                            MultiBufferSource bufferSource) {
        float time = (animatable.getLevel() == null ? 0L : animatable.getLevel().getGameTime()) + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.5, MARK_Y + Mth.sin(time * 0.1F) * 0.08, 0.5);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        float scale = MARK_SCALE * (1.0F + Mth.sin(time * 0.2F) * 0.08F);
        poseStack.scale(scale, -scale, scale);
        float x = -font.width(MARK) / 2.0F;
        font.drawInBatch(MARK, x, 0, 0xFFFFFFFF, true, poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
