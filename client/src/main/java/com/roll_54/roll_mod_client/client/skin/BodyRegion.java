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
    },
    LEFT_ARM {
        @Override
        public List<ModelPart> parts(PlayerModel<?> model) {
            return List.of(model.leftArm, model.leftSleeve);
        }
    },
    RIGHT_LEG {
        @Override
        public List<ModelPart> parts(PlayerModel<?> model) {
            return List.of(model.rightLeg, model.rightPants);
        }
    },
    LEFT_LEG {
        @Override
        public List<ModelPart> parts(PlayerModel<?> model) {
            return List.of(model.leftLeg, model.leftPants);
        }
    };

    /** The model parts this region draws the overlay texture onto, in draw order. */
    public abstract List<ModelPart> parts(PlayerModel<?> model);
}
