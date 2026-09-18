package com.roll_54.roll_mod.economy.vendingblock.gui.hud;

import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public class HintOverlay {

  private static final int ColorBCK = 0xC0161616;
  private static final int ColorBDR = 0xD0161616;
  private static final int ColorTXT = 0xFFFFFFFF;
  private static final int ColorERR = 0xFFba3c3c;

  @SubscribeEvent
  public static void onRenderGUI(RenderGuiEvent.Post event) {
    if (ModList.get().isLoaded("jade")) return;

    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null) return;

    HitResult hit = mc.hitResult;
    if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

    BlockEntity blockEntity = mc.level.getBlockEntity(((BlockHitResult) hit).getBlockPos());
    if (!(blockEntity instanceof VendorBlockEntity entity)) return;

    renderHint(event.getGuiGraphics(), entity, mc);
  }

  private static void renderHint(GuiGraphics gui, VendorBlockEntity entity, Minecraft mc) {
    List<Component> lines = new ArrayList<>();
    List<Integer> colors = new ArrayList<>();

    if (entity.getOwnerUser() != null) {
      lines.add(Component.literal(entity.getOwnerUser()));
      colors.add(ColorTXT);
    }

    int positions = entity.getPositions().size();
    lines.add(Component.translatable("hint.roll_mod.positions", positions));
    colors.add(ColorTXT & 0xCCFFFFFF);

    if (entity.hasError) {
      lines.add(getErrorString(entity.errorCode));
      colors.add(ColorERR);
    }

    if (lines.isEmpty()) return;

    int lineHeight = mc.font.lineHeight + 2;
    int maxWidth = 0;
    for (Component line : lines) maxWidth = Math.max(maxWidth, mc.font.width(line));

    int margin = 8;
    int w = maxWidth + margin * 2;
    int h = lines.size() * lineHeight + margin * 2;
    int x = (mc.getWindow().getGuiScaledWidth() - w) / 2;
    int y = 8;

    drawBackground(gui, x, y, w, h);
    int ty = y + margin;
    for (int i = 0; i < lines.size(); i++) {
      String text = lines.get(i).getString();
      int tx = x + (w - mc.font.width(text)) / 2;
      gui.drawString(mc.font, text, tx, ty, colors.get(i));
      ty += lineHeight;
    }
  }

  private static void drawBackground(GuiGraphics gui, int x, int y, int w, int h) {
    gui.fill(x, y, x + w, y + h, ColorBCK);
    gui.fill(x, y, x + w, y + 1, ColorBDR);
    gui.fill(x, y + h - 1, x + w, y + h, ColorBDR);
    gui.fill(x, y, x + 1, y + h, ColorBDR);
    gui.fill(x + w - 1, y, x + w, y + h, ColorBDR);
  }

  public static Component getErrorString(int code) {
    if (code == 1) return Component.translatable("hint.roll_mod.error.sold");
    else if (code == 2) return Component.translatable("hint.roll_mod.error.full");
    else if (code == 3) return Component.translatable("hint.roll_mod.error.empty");
    else return Component.literal("");
  }
}
