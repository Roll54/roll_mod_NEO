package com.roll_54.roll_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.roll_54.roll_mod.minestar.theme.SquareTabs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Vanilla sprites the theme redraws: the creative tabs square while "Square corners" is on ({@link SquareTabs}), and
 * the anvil's name field in the theme's own field art on a skinned anvil.
 */
@Mixin(GuiGraphics.class)
public abstract class SquareTabsMixin {

    @WrapMethod(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V")
    private void roll_mod$squareTab(ResourceLocation sprite, int x, int y, int w, int h, Operation<Void> original) {
        // The anvil's name field on a themed anvil: the theme's own text field, not vanilla's brown one recoloured.
        if ("minecraft".equals(sprite.getNamespace()) && sprite.getPath().startsWith("container/anvil/text_field")) {
            net.minecraft.client.gui.screens.Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
            com.roll_54.roll_mod.minestar.theme.ThemeArt.Art art = screen != null
                    && com.roll_54.roll_mod.minestar.containers.ContainerSkin.themedByDefault(screen)
                    ? com.roll_54.roll_mod.minestar.theme.ThemeArt.current() : null;
            if (art != null) {
                GuiGraphics graphics = (GuiGraphics) (Object) this;
                // Vanilla's anvil texture has a red strip under the field that vanilla's square field hid; the
                // theme's field has rounded corners, so the panel's colour goes under it first.
                com.roll_54.roll_mod.minestar.theme.UiTheme.Recolor recolor =
                        com.roll_54.roll_mod.minestar.theme.UiThemeState.get().recolor();
                if (recolor != null) {
                    com.roll_54.roll_mod.minestar.theme.GuiRecolor.withoutRecolor(
                            () -> graphics.fill(x, y, x + w, y + h, recolor.surface()));
                }
                com.roll_54.roll_mod.minestar.theme.ThemeArt.draw(graphics, art.field(), x, y, w, h);
                return;
            }
        }
        // An empty slot's icon, light on a dark theme (SlotIcons), in its true colour.
        ResourceLocation icon = com.roll_54.roll_mod.minestar.theme.SlotIcons.replacement(sprite);
        if (icon != null) {
            GuiGraphics graphics = (GuiGraphics) (Object) this;
            com.roll_54.roll_mod.minestar.theme.GuiRecolor.withoutRecolor(() -> graphics.blit(icon, x, y, 0, 0, w, h, w, h));
            return;
        }
        if (SquareTabs.isTab(sprite) && SquareTabs.active()) {
            ResourceLocation square = SquareTabs.square(sprite);
            if (square != null) {
                ((GuiGraphics) (Object) this).blit(square, x, y, 0, 0, w, h, w, h);
                return;
            }
        }
        original.call(sprite, x, y, w, h);
    }
}
