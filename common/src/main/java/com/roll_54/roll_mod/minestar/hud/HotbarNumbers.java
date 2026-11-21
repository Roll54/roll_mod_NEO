package com.roll_54.roll_mod.minestar.hud;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Each hotbar slot's key in its top-left corner — normally 1 to 9, or whatever the player rebound
 * the slot to. Half-size, drawn over the items.
 * <p>Deprecated: no longer subscribed (the bus rejects a subscriber with no listeners); kept for reuse.
 */
@Deprecated
@OnlyIn(Dist.CLIENT)
public final class HotbarNumbers {

    private HotbarNumbers() {}

    /**
     * Not registered any more: the hotbar shows no slot keys (as asked). The drawing below is kept, unused, should
     * they come back.
     */
    @SuppressWarnings("unused")
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, RollMod.id("hotbar_numbers"), HotbarNumbers::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        int left = graphics.guiWidth() / 2 - 91;
        int top = graphics.guiHeight() - 22;
        graphics.pose().pushPose();
        // Above the items, which the hotbar draws well forward.
        graphics.pose().translate(0, 0, 300);
        for (int i = 0; i < 9; i++) {
            Component key = mc.options.keyHotbarSlots[i].getTranslatedKeyMessage();
            String text = key.getString();
            if (text.length() > 2) text = String.valueOf(i + 1);
            graphics.pose().pushPose();
            graphics.pose().translate(left + i * 20 + 3, top + 3, 0);
            graphics.pose().scale(0.5f, 0.5f, 1f);
            graphics.drawString(mc.font, text, 0, 0, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }
}
