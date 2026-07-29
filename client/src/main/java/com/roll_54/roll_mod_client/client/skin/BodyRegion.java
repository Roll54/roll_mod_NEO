package com.roll_54.roll_mod_client.client.skin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.List;

/**
 * A logical region of the player model that a cyberware overlay is drawn onto. Each region resolves
 * to the concrete {@link ModelPart}s (base part + its second/wear layer) that the overlay texture is
 * re-rendered on top of. Kept as an enum so future cyberware (legs, skin, ...) is a one-line addition.
 */
public enum BodyRegion {
    RIGHT_ARM {
        @Override
        public List<ModelPart> parts(PlayerModel<?> model) {
            return List.of(model.rightArm, model.rightSleeve);
        }

        @Override
        public List<ModelPart> secondLayerParts(PlayerModel<?> model) {
            return List.of(model.rightSleeve);
        }

        @Override
        public List<ModelPart> bodyOverlayParts(PlayerModel<?> model) {
            return List.of(model.body);
        }
    },
    LEFT_ARM {
        @Override
        public List<ModelPart> parts(PlayerModel<?> model) {
            return List.of(model.leftArm, model.leftSleeve);
        }

        @Override
        public List<ModelPart> secondLayerParts(PlayerModel<?> model) {
            return List.of(model.leftSleeve);
        }

        @Override
        public List<ModelPart> bodyOverlayParts(PlayerModel<?> model) {
            return List.of(model.body);
        }
    },
    RIGHT_LEG {
        @Override
        public List<ModelPart> parts(PlayerModel<?> model) {
            return List.of(model.rightLeg, model.rightPants);
        }

        @Override
        public List<ModelPart> secondLayerParts(PlayerModel<?> model) {
            return List.of(model.rightPants);
        }
    },
    LEFT_LEG {
        @Override
        public List<ModelPart> parts(PlayerModel<?> model) {
            return List.of(model.leftLeg, model.leftPants);
        }

        @Override
        public List<ModelPart> secondLayerParts(PlayerModel<?> model) {
            return List.of(model.leftPants);
        }
    };

    /** The model parts this region draws the overlay texture onto, in draw order. */
    public abstract List<ModelPart> parts(PlayerModel<?> model);

    /**
     * Extra parts (the torso) that this region's overlay texture also paints, but which are NOT part
     * of the limb itself. A limb texture uses the full player-skin layout, so an arm's shoulder/chest
     * connection lands on the body UV and must be drawn on {@code model.body} too. Only the third-person
     * {@link CyberwareSkinLayer} draws these; first-person rendering ignores them (the torso is
     * off-screen). Empty by default (legs paint no torso pixels).
     */
    public List<ModelPart> bodyOverlayParts(PlayerModel<?> model) {
        return List.of();
    }

    /**
     * The "wear"/second-layer parts of this region (sleeve, pants). roll_mod's overlay textures leave
     * the second-layer UV region transparent, so the vanilla skin overlay drawn there must be hidden
     * when a cyberware overlay is installed — otherwise it bleeds through the cyberlimb.
     */
    public abstract List<ModelPart> secondLayerParts(PlayerModel<?> model);
}
