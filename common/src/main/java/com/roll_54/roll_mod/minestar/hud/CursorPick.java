package com.roll_54.roll_mod.minestar.hud;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * What is under the mouse cursor while {@link FreeCursor} has it free: a ray from the camera through
 * the cursor, found by running the cursor's screen position back through the last frame's projection
 * and view, then cast like vanilla's crosshair pick — blocks to the player's block reach, entities to
 * their entity reach, both measured from the eyes, nearest wins.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class CursorPick {

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    private static boolean captured;

    private CursorPick() {}

    /** The world's matrices for this frame, from the stage where they are final. */
    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        PROJECTION.set(event.getProjectionMatrix());
        VIEW.set(event.getModelViewMatrix());
        captured = true;
    }

    /** Replaces the crosshair's target with the cursor's. Called right after vanilla's own pick. */
    public static void apply(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!captured || player == null || mc.level == null) return;

        double width = mc.getWindow().getScreenWidth();
        double height = mc.getWindow().getScreenHeight();
        if (width <= 0 || height <= 0) return;
        float ndcX = (float) (2 * mc.mouseHandler.xpos() / width - 1);
        float ndcY = (float) (1 - 2 * mc.mouseHandler.ypos() / height);
        Matrix4f inverse = new Matrix4f(PROJECTION).mul(VIEW).invert();
        Vector4f near = inverse.transform(new Vector4f(ndcX, ndcY, -1, 1));
        Vector4f far = inverse.transform(new Vector4f(ndcX, ndcY, 1, 1));
        near.div(near.w);
        far.div(far.w);

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 origin = camera.getPosition().add(near.x, near.y, near.z);
        Vec3 dir = new Vec3(far.x - near.x, far.y - near.y, far.z - near.z).normalize();

        Vec3 eye = player.getEyePosition();
        double blockReach = player.blockInteractionRange();
        double entityReach = player.entityInteractionRange();
        // From the camera, long enough to reach past the player in third person.
        double length = origin.distanceTo(eye) + Math.max(blockReach, entityReach);
        Vec3 end = origin.add(dir.scale(length));

        BlockHitResult block = mc.level.clip(new ClipContext(origin, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        boolean blockInReach = block.getType() != HitResult.Type.MISS
                && block.getLocation().distanceToSqr(eye) <= blockReach * blockReach;
        double blockDist = blockInReach ? block.getLocation().distanceToSqr(origin) : length * length;

        AABB sweep = new AABB(origin, end).inflate(1.0);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, origin, end, sweep,
                e -> !e.isSpectator() && e.isPickable(), blockDist);
        if (entity != null && entity.getLocation().distanceToSqr(eye) <= entityReach * entityReach) {
            mc.hitResult = entity;
            Entity target = entity.getEntity();
            mc.crosshairPickEntity = target;
            return;
        }
        mc.crosshairPickEntity = null;
        mc.hitResult = blockInReach ? block
                : BlockHitResult.miss(end, Direction.getNearest(dir.x, dir.y, dir.z), BlockPos.containing(end));
    }
}
