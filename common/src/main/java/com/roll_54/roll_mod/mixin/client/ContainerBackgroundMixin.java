package com.roll_54.roll_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.roll_54.roll_mod.minestar.containers.ContainerSkin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The two GUI-drawing hooks of the overlay container skin ({@link ContainerSkin}):
 * <ul>
 *   <li>an overlay-skinned container's full-size background texture (blitted at the screen's corner
 *       from 0,0) is skipped — the skin was drawn just before, so the progress bars, lists and previews
 *       vanilla draws next sit on it;</li>
 *   <li>vanilla's dark-grey label colour becomes the theme's text colour on skinned screens, so labels
 *       stay readable on dark themes.</li>
 * </ul>
 * Both only act on a skinned screen; everything else passes straight through.
 */
@Mixin(GuiGraphics.class)
public abstract class ContainerBackgroundMixin {

    /**
     * Every texture blit ends here (the 6-int overload delegates to it). The whole method is wrapped,
     * not cancelled at its head: other mods' hooks inside it that set state up and restore it at the
     * end then either both run or neither does. Cancelling at the head skipped their restore and left
     * GL state behind for everything vanilla drew next (the loom's pattern list vanished).
     */
    @WrapMethod(method = "blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V")
    private void roll_mod$skipBackground(ResourceLocation atlas, int x, int y, int blitOffset, float u, float v,
                                         int w, int h, int texW, int texH, Operation<Void> original) {
        if (u == 0 && v == 0 && ContainerSkin.isOverlayBackground(x, y, w, h)) return;
        original.call(atlas, x, y, blitOffset, u, v, w, h, texW, texH);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int roll_mod$labelColor(int color) {
        return ContainerSkin.labelColor(color);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int roll_mod$labelColorSequence(int color) {
        return ContainerSkin.labelColor(color);
    }
}
