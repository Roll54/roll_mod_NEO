package com.roll_54.roll_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.minestar.containers.ContainerSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * On a container screen {@link ContainerSkin} draws in LdLib, leaves out the two things vanilla draws
 * that the skin replaces: the background texture ({@code renderBg}) and the two labels. Slots, items,
 * the carried stack, tooltips and every other mod's drawing are untouched. On an overlay-skinned screen
 * the skin is drawn just before vanilla's background pass instead.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerSkinMixin {

    @WrapOperation(method = "renderBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
    private void roll_mod$skipBackground(AbstractContainerScreen<?> screen, GuiGraphics graphics, float partialTick,
                                         int mouseX, int mouseY, Operation<Void> original) {
        if (ContainerSkin.isFullSkin(screen)) return;
        // Overlay-skinned: the skin goes down first, and vanilla's own pass draws over it (its
        // background texture skipped by ContainerBackgroundMixin).
        ContainerSkin.drawOverlaySkin(screen, graphics);
        original.call(screen, graphics, partialTick, mouseX, mouseY);
    }

    @WrapOperation(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void roll_mod$skipLabels(AbstractContainerScreen<?> screen, GuiGraphics graphics, int mouseX, int mouseY,
                                     Operation<Void> original) {
        if (!ContainerSkin.isFullSkin(screen)) original.call(screen, graphics, mouseX, mouseY);
    }
}
