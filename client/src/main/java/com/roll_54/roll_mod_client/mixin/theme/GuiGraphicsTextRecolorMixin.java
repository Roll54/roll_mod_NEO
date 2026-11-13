package com.roll_54.roll_mod_client.mixin.theme;

import com.roll_54.roll_mod.minestar.theme.GuiRecolor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Every window's text, not only container labels ({@link AbstractContainerScreenLabelMixin}): whatever the game or
 * a mod writes in vanilla's fixed dark grey ({@code 0x404040}) takes the theme's text colour while "Use the hub
 * theme everywhere" is on — book screens, other mods' panels, settings pages. Other colours are left alone, so
 * coloured text keeps its meaning. {@link GuiRecolor#labelColor} answers vanilla's own value when the theme is
 * not forced, or on the mod's own LdLib screens.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTextRecolorMixin {

    private static final int VANILLA_GREY = 0x404040;

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private int roll_mod$themeString(int color) {
        return recolor(color);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private int roll_mod$themeSequence(int color) {
        return recolor(color);
    }

    private static int recolor(int color) {
        if ((color & 0xFFFFFF) != VANILLA_GREY) return color;
        int alpha = color & 0xFF000000;
        return (alpha == 0 ? 0xFF000000 : alpha) | (GuiRecolor.labelColor(VANILLA_GREY) & 0xFFFFFF);
    }
}
