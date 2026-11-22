package com.roll_54.roll_mod_client.mixin.theme;

import com.roll_54.roll_mod.minestar.theme.GuiRecolor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the HUD out of "Use the hub theme everywhere": the hotbar, hearts, armour and food are drawn as vanilla (and the
 * mod's own tinted hearts) draw them, whether or not a screen such as chat is open over the world. See
 * {@link GuiRecolor#setDrawingHud}.
 */
@Mixin(Gui.class)
public abstract class GuiHudNoRecolorMixin {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void roll_mod$hudStart(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        GuiRecolor.setDrawingHud(true);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void roll_mod$hudEnd(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        GuiRecolor.setDrawingHud(false);
    }
}
