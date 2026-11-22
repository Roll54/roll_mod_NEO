package com.roll_54.roll_mod.mixin.client;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.roll_54.roll_mod.minestar.theme.LightText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Flat panel fills pass through {@link LightText#fill}: the AE2 theme's panels stay AE2-coloured, not grey. */
@Mixin(value = ColorRectTexture.class, remap = false)
public abstract class LdLibColorRectMixin {

    @Redirect(method = "drawInternal", at = @At(value = "FIELD",
            target = "Lcom/lowdragmc/lowdraglib2/gui/texture/ColorRectTexture;color:I"), remap = false, require = 0)
    private int roll_mod$fill(ColorRectTexture self) {
        return LightText.fill(self.color);
    }
}
