package com.roll_54.roll_mod_client.mixin.theme;

import com.roll_54.roll_mod.minestar.theme.GuiRecolor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Vanilla writes a container's title and "Inventory" in a fixed dark grey ({@code 0x404040}), which
 * is unreadable once {@link GuiRecolor} has turned the panel dark. While the theme is forced, the
 * labels take the theme's text colour instead.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenLabelMixin {

    // require = 0: experimental; a miss only leaves the labels vanilla grey.
    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 4210752), require = 0)
    private int roll_mod$themeLabel(int vanilla) {
        return GuiRecolor.labelColor(vanilla);
    }
}
