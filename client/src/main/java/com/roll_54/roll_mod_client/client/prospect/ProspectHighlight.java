package com.roll_54.roll_mod_client.client.prospect;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.roll_54.roll_mod.prospecting.ClientProspects;
import com.roll_54.roll_mod.prospecting.Discovery;
import com.roll_54.roll_mod.prospecting.ProspectPacket;
import com.roll_54.roll_mod_client.RollModClient;
import com.roll_54.roll_mod_client.compat.xaero.ProspectMapState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.OptionalDouble;

/**
 * What the pick did, outlined through the terrain for {@link ClientProspects#HIGHLIGHT_MILLIS}, fading out over its
 * last second, in place of its particle cloud: the column's first blocks in the pick's mint green, and the ore
 * blocks the prospect found in each ore's scanner colour.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class ProspectHighlight {

    private static final long FADE_MILLIS = 1_000;
    /** The pick's mint green, as its particles were (0.243, 0.902, 0.690). */
    private static final float MINT_R = 0.243f, MINT_G = 0.902f, MINT_B = 0.690f;
    /** Past this many blocks from the camera an outline is not drawn. */
    private static final double MAX_DISTANCE = 96;

    private ProspectHighlight() {}

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        long now = System.currentTimeMillis();
        long left = ClientProspects.highlightUntil - now;
        long columnLeft = ClientProspects.columnHit == null ? 0 : ClientProspects.columnUntil - now;
        if ((left <= 0 || ClientProspects.highlight.isEmpty()) && columnLeft <= 0) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String dim = mc.level.dimension().location().toString();
        float alpha = fade(left);

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(Types.LINES_THROUGH);
        double maxSq = MAX_DISTANCE * MAX_DISTANCE;
        if (columnLeft > 0) column(mc, pose, lines, fade(columnLeft));
        if (left > 0) for (ProspectPacket.ChunkFinds c : ClientProspects.highlight) {
            if (!c.dimension().equals(dim)) continue;
            int bx = ChunkPos.getX(c.chunk()) << 4, bz = ChunkPos.getZ(c.chunk()) << 4;
            for (Discovery d : c.ores()) {
                int rgb = ProspectMapState.color(d);
                float r = (rgb >> 16 & 0xFF) / 255f, g = (rgb >> 8 & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
                for (int packed : d.positions) {
                    int x = bx + Discovery.x(packed), y = Discovery.y(packed), z = bz + Discovery.z(packed);
                    if (cam.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) > maxSq) continue;
                    LevelRenderer.renderLineBox(pose, lines, x, y, z, x + 1, y + 1, z + 1, r, g, b, alpha);
                }
            }
        }
        pose.popPose();
        buffers.endBatch(Types.LINES_THROUGH);
    }

    private static float fade(long left) {
        return left <= 0 ? 0f : left >= FADE_MILLIS ? 1f : left / (float) FADE_MILLIS;
    }

    /**
     * The first {@link ClientProspects#COLUMN_DEPTH} layers of the prospected column, from the block hit into the
     * direction it runs, every solid block outlined in the pick's mint green: where its particle cloud used to be.
     */
    private static void column(Minecraft mc, PoseStack pose, VertexConsumer lines, float alpha) {
        var hit = ClientProspects.columnHit;
        int r = ClientProspects.columnRadius, step = ClientProspects.columnUp ? 1 : -1;
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int i = 0; i < ClientProspects.COLUMN_DEPTH; i++) {
            int y = hit.getY() + i * step;
            for (int x = hit.getX() - r; x <= hit.getX() + r; x++) {
                for (int z = hit.getZ() - r; z <= hit.getZ() + r; z++) {
                    if (mc.level.getBlockState(pos.set(x, y, z)).isAir()) continue;
                    LevelRenderer.renderLineBox(pose, lines, x, y, z, x + 1, y + 1, z + 1,
                            MINT_R, MINT_G, MINT_B, alpha);
                }
            }
        }
    }

    /** Lines drawn over the terrain (no depth test), so ore behind stone shows. */
    private static final class Types extends RenderType {
        static final RenderType LINES_THROUGH = create("roll_mod_prospect_lines",
                DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536, false, false,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(2.0)))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .createCompositeState(false));

        private Types(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling,
                      boolean sort, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sort, setup, clear);
        }
    }
}
