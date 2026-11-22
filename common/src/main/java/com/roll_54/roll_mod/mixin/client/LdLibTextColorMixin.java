package com.roll_54.roll_mod.mixin.client;

import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.roll_54.roll_mod.minestar.theme.LightText;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Supplier;

/**
 * Every LdLib text draw ends in this one method; on a light theme its colours and shadow go through {@link LightText}.
 * Optional ({@code require = 0}): an LdLib without this method just keeps its own colours.
 */
@Mixin(value = LDLibFonts.class, remap = false)
public abstract class LdLibTextColorMixin {

    private static final String DRAW = "drawText(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;"
            + "Ljava/lang/Object;Ljava/util/function/Supplier;FFIZ)I";

    @ModifyVariable(method = DRAW, at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 0)
    private static Object roll_mod$key(Object text) {
        return LightText.active() ? new LightText.Key(text, UiThemeState.get()) : text;
    }

    @SuppressWarnings("rawtypes")
    @ModifyVariable(method = DRAW, at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 0)
    private static Supplier roll_mod$text(Supplier text) {
        return LightText.active() ? LightText.wrap(text) : text;
    }

    @ModifyVariable(method = DRAW, at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 0)
    private static int roll_mod$color(int color) {
        return LightText.active() ? LightText.color(color) : color;
    }

    @ModifyVariable(method = DRAW, at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 0)
    private static boolean roll_mod$shadow(boolean shadow) {
        return !LightText.active() && shadow;
    }

    /**
     * Text on whole pixels: a caption centred in a box of odd size lands half a pixel off, and the font then draws
     * some glyph pixels doubled and others dropped ("broken" letters). x and y are rounded to the nearest pixel.
     */
    @ModifyVariable(method = DRAW, at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 0)
    private static float roll_mod$x(float x) {
        return Math.round(x);
    }

    @ModifyVariable(method = DRAW, at = @At("HEAD"), argsOnly = true, ordinal = 1, remap = false, require = 0)
    private static float roll_mod$y(float y) {
        return Math.round(y);
    }
}
