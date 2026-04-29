package com.roll_54.roll_mod.registry;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class KeyMappingRegistry {

    public static final Lazy<KeyMapping> CHESTPLATE_TOGGLE_ONE = Lazy.of(() -> new KeyMapping(
            "key.roll_mod.gravichestplate.chestplate_toggle_one",
            GLFW.GLFW_KEY_Z,
            "key.categories.roll_mod"
    ));
    public static final Lazy<KeyMapping> CHESTPLATE_TOGGLE_TWO = Lazy.of(() -> new KeyMapping(
            "key.roll_mod.gravichestplate.chestplate_toggle_two",
            GLFW.GLFW_KEY_X,
            "key.categories.roll_mod"
    ));
    public static final Lazy<KeyMapping> CHESTPLATE_TOGGLE_THREE = Lazy.of(() -> new KeyMapping(
            "key.roll_mod.gravichestplate.chestplate_toggle_three",
            GLFW.GLFW_KEY_C,
            "key.categories.roll_mod"
    ));

}
