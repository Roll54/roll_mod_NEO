package com.roll_54.roll_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.minestar.theme.ThemeArt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Vanilla's slider in the hub theme: the track as a field, the handle as a button. See {@link ThemeArt}. */
@Mixin(AbstractSliderButton.class)
public abstract class SliderThemeMixin {

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void roll_mod$themedTrack(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int w, int h,
                                      Operation<Void> original) {
        ThemeArt.Art art = ThemeArt.active();
        if (art == null) original.call(graphics, sprite, x, y, w, h);
        else ThemeArt.draw(graphics, art.field(), x, y, w, h);
    }

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void roll_mod$themedHandle(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int w, int h,
                                       Operation<Void> original) {
        ThemeArt.Art art = ThemeArt.active();
        if (art == null) {
            original.call(graphics, sprite, x, y, w, h);
            return;
        }
        AbstractWidget self = (AbstractWidget) (Object) this;
        ThemeArt.draw(graphics, !self.active ? art.buttonDisabled() : self.isHoveredOrFocused() ? art.buttonHover() : art.button(),
                x, y, w, h);
    }

    @ModifyArg(method = "renderWidget", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractSliderButton;renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;II)V"),
            index = 3)
    private int roll_mod$themedLabel(int color) {
        return ThemeArt.active() == null ? color : ThemeArt.text(color);
    }
}
