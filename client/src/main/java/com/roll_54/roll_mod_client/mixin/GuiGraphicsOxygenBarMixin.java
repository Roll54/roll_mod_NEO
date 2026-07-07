package com.roll_54.roll_mod_client.mixin;

import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.common_storage_lib.resources.ResourceStack;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.swedz.extended_industrialization.item.nanosuit.NanoSuitArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a second inventory bar for Extended Industrialization's nano suit, showing its oxygen tank
 * (added by {@code NanoArmorMixin}) just above the item's native energy bar. Rendered after the vanilla
 * durability bar so it sits on top; mirrors Ad Astra's {@code SpaceSuitItem} bar math.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsOxygenBarMixin {

    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL")
    )
    private void roll_mod$renderOxygenBar(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (!(stack.getItem() instanceof NanoSuitArmorItem)) {
            return;
        }
        if (!FluidUtils.hasFluid(stack)) {
            return;
        }

        ResourceStack<FluidResource> tank = FluidUtils.getTank(stack);
        long capacity = FluidUtils.getTankCapacity(stack);
        if (capacity <= 0) {
            return;
        }

        int width = (int) ((double) tank.amount() / (double) capacity * 13.0);
        int color = ClientPlatformUtils.getFluidColor(tank) | 0xFF000000;

        GuiGraphics gui = (GuiGraphics) (Object) this;
        int barX = x + 2;
        int barY = y + 11; // 2px above the vanilla durability/energy bar at y+13

        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 235.0F); // draw above the item model, like the vanilla bar
        gui.fill(barX, barY, barX + 13, barY + 2, 0xFF000000);
        gui.fill(barX, barY, barX + width, barY + 1, color);
        gui.pose().popPose();
    }
}
