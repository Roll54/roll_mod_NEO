package com.roll_54.roll_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.minestar.theme.LightText;
import com.roll_54.roll_mod.minestar.theme.ThemeArt;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Vanilla's text box in the hub theme's field art, its text in the theme's colour on light themes. See {@link ThemeArt}. */
@Mixin(EditBox.class)
public abstract class EditBoxThemeMixin {

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void roll_mod$themedField(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int w, int h,
                                      Operation<Void> original) {
        ThemeArt.Art art = ThemeArt.forScreen();
        if (art == null) original.call(graphics, sprite, x, y, w, h);
        else ThemeArt.draw(graphics, art.field(), x, y, w, h);
    }

    @ModifyArg(method = "renderWidget", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"),
            index = 4)
    private int roll_mod$themedText(int color) {
        return ThemeArt.forScreen() == null ? color : ThemeArt.text(color);
    }

    @ModifyArg(method = "renderWidget", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"),
            index = 4)
    private int roll_mod$themedStringText(int color) {
        return ThemeArt.forScreen() == null ? color : ThemeArt.text(color);
    }

    /**
     * The empty field's hint ("Пошук..." in JEI's and EMI's search bars): drawn through the {@code Component} overload,
     * which the two above miss, and usually in a styled grey that wins over the colour argument. On a light theme both
     * go through the theme's text mapping, as the typed text does, so the hint no longer shows in vanilla's grey with a
     * light shadow on the theme's field.
     */
    @WrapOperation(method = "renderWidget", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
    private int roll_mod$themedHint(GuiGraphics graphics, Font font, Component text, int x, int y, int color,
                                    boolean shadow, Operation<Integer> original) {
        if (ThemeArt.forScreen() == null) return original.call(graphics, font, text, x, y, color, shadow);
        if (LightText.active()) {
            return graphics.drawString(font, LightText.sequence(text.getVisualOrderText()), x, y, ThemeArt.text(color), false);
        }
        // Dark themes: the theme's secondary text colour, as the hub's own fields show their hints, not vanilla's grey.
        int dim = com.roll_54.roll_mod.minestar.theme.UiThemeState.get().palette().dim();
        return graphics.drawString(font, text.getString(), x, y, dim, false);
    }
}
