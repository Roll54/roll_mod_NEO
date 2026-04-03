package com.roll_54.roll_mod.mixin;

import aztech.modern_industrialization.MIText;
import aztech.modern_industrialization.client.items.armor.HudRenderer;
import aztech.modern_industrialization.config.MIClientConfig;
import aztech.modern_industrialization.items.FluidFuelItemHelper;
import aztech.modern_industrialization.items.armor.GraviChestPlateItem;
import aztech.modern_industrialization.items.armor.JetpackItem;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(HudRenderer.class)
public class ModifyEnergyHudRendererMixin {
    /**
     * @author roll_54
     * @reason Idiont on Dev don't understand that hardcode is a BAD thing.
     */
    @Overwrite
    public static void onRenderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, (float) MIClientConfig.INSTANCE.armorHudYPosition.getAsInt(), 0.0F);
            ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            Item active = chest.getItem();
            Item chestItem = chest.getItem();
            if (chestItem instanceof JetpackItem jetpack) {
                boolean isActive = jetpack.isActivated(chest);

                MutableComponent jetpackActiveComponent;
                if (isActive) {
                    jetpackActiveComponent = MIText.JetpackEnabled.text()
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                } else {
                    jetpackActiveComponent = MIText.JetpackDisabled.text()
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                }

                guiGraphics.drawString(mc.font, jetpackActiveComponent, 4, 0, 16383998);

                Component fillText = MIText.JetpackFill.text(
                        FluidFuelItemHelper.getAmount(chest) * 100 / 8000
                );
                guiGraphics.drawString(mc.font, fillText, 4, 10, 16383998);

            } else if (chestItem instanceof GraviChestPlateItem gsp) {

                boolean isActive = gsp.isActivated(chest);

                MutableComponent gravichestplateActiveComponent;
                if (isActive) {
                    gravichestplateActiveComponent = MIText.GravichestplateEnabled.text()
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                } else {
                    gravichestplateActiveComponent = MIText.GravichestplateDisabled.text()
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                }

                guiGraphics.drawString(mc.font, gravichestplateActiveComponent, 4, 0, 16383998);

                Component fillText = MIText.EnergyFill.text(
                        gsp.getEnergy(chest) * 100L / (1 << 30)
                );
                guiGraphics.drawString(mc.font, fillText, 4, 10, 16383998);
            }

            guiGraphics.pose().popPose();
        }

    }
}
