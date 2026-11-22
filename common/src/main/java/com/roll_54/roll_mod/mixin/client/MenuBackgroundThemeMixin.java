package com.roll_54.roll_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Deprecated: the menu background used to be the theme's panel. Vanilla's see-through background (the blurred panorama
 * or world behind it) is left as it is now — themes do not touch the translucent parts — so this only passes through.
 * Kept, not deleted, so it can be brought back.
 */
@Deprecated
@Mixin(Screen.class)
public abstract class MenuBackgroundThemeMixin {

    @WrapMethod(method = "renderMenuBackgroundTexture")
    private static void roll_mod$themedBackground(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                                                  float u, float v, int w, int h, Operation<Void> original) {
        // Never recoloured either: the theme leaves vanilla's translucent background alone.
        com.roll_54.roll_mod.minestar.theme.GuiRecolor.withoutRecolor(() -> original.call(graphics, texture, x, y, u, v, w, h));
    }
}
