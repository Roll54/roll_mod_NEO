package com.roll_54.roll_mod.compat.curios;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.client.gui.CuriosButton;
import top.theillusivec4.curios.common.network.client.CPacketOpenCurios;

/**
 * The client-side Curios calls roll_mod's screens make. Names Curios types, so only call it after
 * {@link CuriosCompat#loaded()} said yes.
 */
@OnlyIn(Dist.CLIENT)
public final class CuriosClientAccess {

    private CuriosClientAccess() {}

    /** Opens Curios' screen, as its own inventory button does: the server opens the menu. */
    public static void openCurios(ItemStack carried) {
        PacketDistributor.sendToServer(new CPacketOpenCurios(carried));
    }

    /** Curios' own button icon. */
    public static void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered) {
        graphics.blitSprite(CuriosButton.BIG.get(true, hovered), x, y, w, h);
    }
}
