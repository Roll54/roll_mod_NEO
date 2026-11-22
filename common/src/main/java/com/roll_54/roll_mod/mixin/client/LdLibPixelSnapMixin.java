package com.roll_54.roll_mod.mixin.client;

import com.lowdragmc.lowdraglib2.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On AE2 and the Ore themes every LdLib texture is drawn at whole pixels. AE2's art is a 1px outline round a 1px light edge;
 * a button that lands between pixels (two buttons sharing a row's width) had its outline round one way and its light
 * edge the other, which showed as stray white pixels or a 2px outline. The edges are snapped inwards, not the size
 * rounded: rounding the width on its own pushed the right edge a pixel past where the element ends, and a clipping
 * row then cut the outline off. Optional: an LdLib without this method just draws as it always did.
 */
@Mixin(value = TransformTexture.class, remap = false)
public abstract class LdLibPixelSnapMixin {

    @Inject(method = "draw(Lcom/lowdragmc/lowdraglib2/gui/ui/rendering/GUIContext;FFFF)V", at = @At("HEAD"),
            cancellable = true, remap = false, require = 0)
    private void roll_mod$snap(GUIContext context, float x, float y, float width, float height, CallbackInfo ci) {
        // AE2 and the Ore themes: both are bevelled pixel art that splits the same way between pixels.
        UiTheme theme = UiThemeState.get();
        if (theme != UiTheme.AE2 && !theme.oreArt()) return;
        // Inward: the left and top edges up, the right and bottom down, so the box never reaches past the element
        // (a clipping parent would cut its outline) — a box between pixels loses at most a pixel each side.
        float left = (float) Math.ceil(x - 0.001f), top = (float) Math.ceil(y - 0.001f);
        float right = (float) Math.floor(x + width + 0.001f), bottom = (float) Math.floor(y + height + 0.001f);
        if (right - left < 1 || bottom - top < 1) return;
        if (left == x && top == y && right - left == width && bottom - top == height) return;
        ci.cancel();
        ((TransformTexture) (Object) this).draw(context, left, top, right - left, bottom - top);
    }
}
