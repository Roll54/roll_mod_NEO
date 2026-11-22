package com.roll_54.roll_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.minestar.theme.ThemeArt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Vanilla's button (and every mod button that keeps vanilla's drawing) in the hub theme's own button art — normal,
 * hovered/focused or disabled — instead of vanilla's grey sprite, while "Use the hub theme everywhere" is on. Its
 * label reads in the theme's text colour on the light themes. See {@link ThemeArt}.
 */
@Mixin(AbstractButton.class)
public abstract class VanillaWidgetThemeMixin {

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void roll_mod$themedButton(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int w, int h,
                                       Operation<Void> original) {
        ThemeArt.Art art = this instanceof ThemeArt.KeepsOwnLook ? null : ThemeArt.active();
        // Green OreUI's button art is the grey Ore one: its green lives only in the recolour. There vanilla's own
        // button sprite is drawn instead and recoloured into the same greens as the panel it sits on.
        if (art == null || ThemeArt.buttonsRecoloured()) {
            original.call(graphics, sprite, x, y, w, h);
            return;
        }
        AbstractWidget self = (AbstractWidget) (Object) this;
        if (self.active && self.isHoveredOrFocused()
                && com.roll_54.roll_mod.minestar.theme.UiThemeState.get() == com.roll_54.roll_mod.minestar.theme.UiTheme.AE2) {
            // AE2's hover art (its bevel tinted light blue) lost its dark right and bottom edges on vanilla buttons
            // (NeoForge's creative-tab page arrows): the plain button, frame whole, with the light blue laid inside
            // its bevel (2px sides and top, 4px bottom, as the art's 9-slice).
            ThemeArt.draw(graphics, art.button(), x, y, w, h);
            if (w > 4 && h > 6) graphics.fill(x + 2, y + 2, x + w - 2, y + h - 4, 0x80DFF3FF);
            return;
        }
        ThemeArt.draw(graphics, !self.active ? art.buttonDisabled() : self.isHoveredOrFocused() ? art.buttonHover() : art.button(),
                x, y, w, h);
    }

    /**
     * On the light themes the label turns dark, and vanilla's drop shadow under dark text read as a doubled, smeared
     * label: the label is drawn without its shadow there ({@link ThemeArt#noShadow}).
     */
    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"))
    private void roll_mod$labelWithoutShadow(AbstractButton self, GuiGraphics graphics, net.minecraft.client.gui.Font font,
                                             int color, Operation<Void> original) {
        boolean flat = !(this instanceof ThemeArt.KeepsOwnLook) && ThemeArt.active() != null
                && com.roll_54.roll_mod.minestar.theme.LightText.active();
        if (!flat) {
            original.call(self, graphics, font, color);
            return;
        }
        ThemeArt.noShadow = true;
        try {
            original.call(self, graphics, font, color);
        } finally {
            ThemeArt.noShadow = false;
        }
    }

    @ModifyArg(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"),
            index = 2)
    private int roll_mod$themedLabel(int color) {
        return this instanceof ThemeArt.KeepsOwnLook || ThemeArt.active() == null ? color : ThemeArt.text(color);
    }
}
