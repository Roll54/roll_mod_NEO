package com.roll_54.roll_mod.HUD;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.init.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public class SulfurHUDOverlay {
    private static final ResourceLocation SULFUR_HEARTS =
            ResourceLocation.fromNamespaceAndPath("roll_mod", "textures/gui/sulfur_hearts.png");

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // ✅ використовуємо Holder<MobEffect>
        if (player.getActiveEffectsMap().containsKey(ModEffects.SULFUR_POISONING.get())) {
            GuiGraphics g = event.getGuiGraphics();
            int screenWidth = g.guiWidth();
            int screenHeight = g.guiHeight();

            int x = screenWidth / 2 - 91;
            int y = screenHeight - 39;

            g.blit(SULFUR_HEARTS, x, y, 0, 0, 81, 9);
        }
    }
}