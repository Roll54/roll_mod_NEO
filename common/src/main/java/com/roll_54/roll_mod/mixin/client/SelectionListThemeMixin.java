package com.roll_54.roll_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Deprecated: a vanilla list's well used to be the theme's field art. Vanilla's see-through list background is left as
 * it is now — themes do not touch the translucent parts — so this only passes through. Kept, not deleted.
 */
@Deprecated
@Mixin(AbstractSelectionList.class)
public abstract class SelectionListThemeMixin {

    @WrapMethod(method = "renderListBackground")
    private void roll_mod$themedWell(GuiGraphics graphics, Operation<Void> original) {
        com.roll_54.roll_mod.minestar.theme.GuiRecolor.withoutRecolor(() -> original.call(graphics));
    }
}
