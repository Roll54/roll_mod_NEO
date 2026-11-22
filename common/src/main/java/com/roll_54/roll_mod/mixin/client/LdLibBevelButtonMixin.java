package com.roll_54.roll_mod.mixin.client;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * On AE2 and the Ore themes a button's face is drawn 1px taller above and below its box: their bevelled art (thick
 * bottom edge) read as squashed at the hub's button heights. Only the drawing grows; the layout, and so every row's
 * spacing, stays as it was. Optional: an LdLib without this call draws as it always did.
 */
@Mixin(value = Button.class, remap = false)
public abstract class LdLibBevelButtonMixin {

    @Redirect(method = "drawBackgroundAdditional",
            at = @At(value = "INVOKE",
                    target = "Lcom/lowdragmc/lowdraglib2/gui/ui/rendering/GUIContext;drawTexture(Lcom/lowdragmc/lowdraglib2/gui/texture/IGuiTexture;FFFF)V"),
            remap = false, require = 0)
    private void roll_mod$taller(GUIContext context, IGuiTexture texture, float x, float y, float width, float height) {
        UiTheme theme = UiThemeState.get();
        // A disabled button keeps the size and shape of an enabled one: the theme's own disabled art (smaller, with a
        // thinner frame on some themes) made a row's greyed arrow look shrunk next to the live one. The enabled art,
        // faded, instead.
        if (!((com.lowdragmc.lowdraglib2.gui.ui.elements.Button) (Object) this).isActive()) {
            com.roll_54.roll_mod.minestar.theme.ThemeArt.Art art = com.roll_54.roll_mod.minestar.theme.ThemeArt.current();
            if (art != null) {
                texture = art.button();
                // Flushed first, so only this button is drawn faded, not what LdLib had batched before it.
                context.graphics.flush();
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 0.5f);
                drawSized(context, texture, x, y, width, height, theme);
                context.graphics.flush();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                return;
            }
        }
        drawSized(context, texture, x, y, width, height, theme);
    }

    private static void drawSized(GUIContext context, IGuiTexture texture, float x, float y, float width, float height,
                                  UiTheme theme) {
        if (theme == UiTheme.AE2 || theme.oreArt()) {
            context.drawTexture(texture, x, y - 1, width, height + 2);
        } else {
            context.drawTexture(texture, x, y, width, height);
        }
    }
}
