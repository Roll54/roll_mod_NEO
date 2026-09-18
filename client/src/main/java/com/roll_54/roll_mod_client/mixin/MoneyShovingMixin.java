package com.roll_54.roll_mod_client.mixin;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.config.CurrencyConfig;
import com.roll_54.roll_mod.economy.network.client.ClientCurrencyHolder;
import com.roll_54.roll_mod.economy.util.EnergyFormatUtils;
import com.roll_54.roll_mod.economy.util.RgbFormatting;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class MoneyShovingMixin {

  @Unique
  private static final ResourceLocation LP_ICON =
      RollMod.id("textures/gui/lp.png");

  // Your new background texture
  @Unique
  private static final ResourceLocation PANEL_BG =
      RollMod.id("textures/gui/currency_bg.png");

  @Inject(method = "renderBg", at = @At("TAIL"))
  private void rc$renderCurrencyPanel(
      GuiGraphics gg, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) return;

    int invX = (gg.guiWidth() - 176) / 2;
    int invY = (gg.guiHeight() - 166) / 2;
    int xForAlign = invX;
    int yForAlign = invY - 14;

    // 1. Draw Background
    gg.blit(PANEL_BG, xForAlign + 17, yForAlign, 0, 0, 54, 14, 54, 14);

    // 2. Draw Icon
    gg.blit(LP_ICON, xForAlign - 3, yForAlign - 3, 0, 0, 20, 20, 20, 20);

    // 3. Format the balance using your utility
    long rawBalance = ClientCurrencyHolder.getMainBalance();
    String text = EnergyFormatUtils.formatEnergy(ClientCurrencyHolder.getMainBalance());

    // 4. Draw Animated Gradient Text
    rc$drawGradientString(gg, mc, text, xForAlign + 22, yForAlign + 5);

    int textWidth = mc.font.width(text);
    if (mouseX >= xForAlign + 18
        && mouseX <= xForAlign + 18 + textWidth
        && mouseY >= yForAlign
        && mouseY <= yForAlign + 14) {

      List<Component> tooltipLines = new ArrayList<>();
      TextColor serverColor = TextColor.fromRgb(0xB7043B);

      // todo make case specific tooltip for uk_ua like 2 старкоіни, 1 старкоін, 0 старкоінів

      // 1 line
      Component currencyName =
          Component.translatable("currency.rollcurrency.name")
              .withStyle(style -> style.withColor(serverColor));
      tooltipLines.add(
          Component.translatable("tooltip.roll_mod.name", currencyName)
              .withStyle(ChatFormatting.GREEN));
      // 2 line
      tooltipLines.add(
          Component.translatable("tooltip.roll_mod.full_balance", rawBalance)
              .withStyle(ChatFormatting.WHITE));
      // 3 line
      tooltipLines.add(
          Component.translatable("tooltip.roll_mod.hint").withStyle(ChatFormatting.BLUE));

      gg.renderTooltip(mc.font, tooltipLines, Optional.empty(), mouseX, mouseY);
    }
  }

  @Unique
  private void rc$drawGradientString(GuiGraphics gg, Minecraft mc, String text, int x, int y) {
    // Get gradient colors from configuration
    int startColor = CurrencyConfig.MAIN.client.gradientStartColor.toInt();
    int endColor = CurrencyConfig.MAIN.client.gradientEndColor.toInt();

    // Build gradient component using utility
    Component component = RgbFormatting.buildGradientComponent(text, startColor, endColor);

    gg.drawString(mc.font, component, x, y, 0xFFFFFF, false);
  }
}
