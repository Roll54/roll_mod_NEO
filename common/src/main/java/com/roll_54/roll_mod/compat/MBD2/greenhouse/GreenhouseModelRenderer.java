package com.roll_54.roll_mod.compat.MBD2.greenhouse;

import com.lowdragmc.mbd2.integration.geckolib.GeckolibRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.Direction;

/**
 * The formed greenhouse's geo model. MBD2's renderer centres a model on the controller; this one is
 * centred on the crop cell, one block behind it, because the 3×3×3 model wraps the whole structure
 * and the controller sits in the middle of its front face.
 *
 * <p>Built only on the client (MBD2 calls renderer suppliers there alone).
 */
public class GreenhouseModelRenderer extends GeckolibRenderer {

    public GreenhouseModelRenderer() {
        super(RollMod.id("geo/block/ev_greenhouse.geo.json"),
                RollMod.id("textures/block/ev_greenhouse.png"),
                // Empty: MBD2 draws a geo model only once its animation file is loaded.
                RollMod.id("animations/block/ev_greenhouse.animation.json"));
    }

    /** After turning the model to the controller's facing, shift it back by one block. */
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        super.rotateBlock(facing, poseStack);
        poseStack.translate(0, 0, 1);
    }
}
