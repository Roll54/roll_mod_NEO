package com.roll_54.roll_mod.client.screen;

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
        if (mc.player != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 4, 0.0F);
            ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            Item active = chest.getItem();

                if (active instanceof MultiProtectingGraviChestItem) {

                    MultiProtectingGraviChestItem gsp = (MultiProtectingGraviChestItem) active;

                    boolean active2 = gsp.isActivatedFirst(chest);

                    MutableComponent gravichestplateActiveComponent;

                    if (active2) {
                        gravichestplateActiveComponent = Component.translatable("bukvi").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                    } else {
                        gravichestplateActiveComponent = Component.translatable("bukvi").setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                    }

                    guiGraphics.drawString(mc.font, gravichestplateActiveComponent, 4, 10, 16383998);
                    long energyPercentage = gsp.getStoredEnergy(chest) * 100L / ENERGY_CAPACITY;
                    Component fillText = Component.translatable("bukvi1", energyPercentage);
                    guiGraphics.drawString(mc.font, fillText, 4, 0, 16383998);

                }

                guiGraphics.pose().popPose();

        }
    }
}
