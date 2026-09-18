package com.roll_54.roll_mod_client.registry;

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

    /**
     * Opens the hub, on whichever tab it was last closed on. F4 is unbound in vanilla (F3+F4 is a
     * separate chord).
     *
     * <p>Was the daily-tasks key: every screen it used to open is a hub tab now, so the key opens
     * the hub itself and lands you back where you left it — see {@code HubUI.lastTab}.
     */
    public static final Lazy<KeyMapping> HUB = Lazy.of(() -> new KeyMapping(
            "key.roll_mod.hub",
            GLFW.GLFW_KEY_F4,
            "key.categories.roll_mod"
    ));

}
