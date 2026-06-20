package com.roll_54.roll_mod_client.client.screen;

import com.roll_54.roll_mod.items.armor.geckolib.MultiProtectingGraviChestItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static com.roll_54.roll_mod.items.armor.geckolib.MultiProtectingGraviChestItem.ENERGY_CAPACITY;


public class HudRender {

    public static void onRenderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            return;
        }

        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        Item active = chest.getItem();

        if (!(active instanceof MultiProtectingGraviChestItem gsp)) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 4.0F, 0.0F);

        long energyPercentage = gsp.getStoredEnergy(chest) * 100L / ENERGY_CAPACITY;

        MutableComponent energyText = Component.translatable("hud.roll_mod.energy", energyPercentage)
                .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA));

        String flightStatus = onOff(gsp.isActivatedById(chest, 0));
        MutableComponent flightText = Component.translatable("hud.roll_mod.creative_flight", flightStatus)
                .setStyle(Style.EMPTY.withColor(gsp.isActivatedById(chest, 0) ? ChatFormatting.GREEN : ChatFormatting.GRAY));

        String sulfurStatus = onOff(gsp.isActivatedById(chest, 1));
        MutableComponent sulfurText = Component.translatable("hud.roll_mod.storm_protection", sulfurStatus)
                .setStyle(Style.EMPTY.withColor(gsp.isActivatedById(chest, 1) ? ChatFormatting.GREEN : ChatFormatting.GRAY));

        String elytraStatus = onOff(gsp.isActivatedById(chest, 2));
        MutableComponent elytraText = Component.translatable("hud.roll_mod.elytra_flight", elytraStatus)
                .setStyle(Style.EMPTY.withColor(gsp.isActivatedById(chest, 2) ? ChatFormatting.GREEN : ChatFormatting.GRAY));

        guiGraphics.drawString(mc.font, energyText, 4, 0, 16383998);
        guiGraphics.drawString(mc.font, flightText, 4, 10, 16383998);
        guiGraphics.drawString(mc.font, sulfurText, 4, 20, 16383998);
        guiGraphics.drawString(mc.font, elytraText, 4, 30, 16383998);

        guiGraphics.pose().popPose();
    }

    private static String onOff(boolean value) {
        return value ? net.minecraft.network.chat.Component.translatable("hud.roll_mod.on").getString()
                     : net.minecraft.network.chat.Component.translatable("hud.roll_mod.off").getString();
    }
}
