package com.roll_54.roll_mod_client.compat.tei.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.roll_54.roll_mod.minestar.theme.GuiRecolor;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;

/**
 * TEI's category tabs show each category's icon: an item, or a picture from a mod's sheet (TEI's blue info icon, a
 * mod's dagger, AE2's tool). Those pictures went through the theme recolour like a frame would, and their greys turned
 * into the theme's tones. A tab icon is a picture: it is drawn in its own colours.
 */
@Mixin(targets = "dev.lorp.tei.api.CategoryIcons", remap = false)
public abstract class TeiCategoryIconColoursMixin {

    @WrapMethod(method = "draw")
    private static void roll_mod$ownColours(GuiGraphics g, dev.lorp.tei.api.TeiCategory c,
                                            int x, int y, Operation<Void> original) {
        GuiRecolor.withoutRecolor(() -> original.call(g, c, x, y));
    }
}
