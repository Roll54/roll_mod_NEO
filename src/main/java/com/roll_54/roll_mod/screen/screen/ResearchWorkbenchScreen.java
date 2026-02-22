package com.roll_54.roll_mod.screen.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.roll_54.roll_mod.screen.menu.ResearchWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ResearchWorkbenchScreen extends AbstractContainerScreen<ResearchWorkbenchMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.parse("roll_mod:textures/gui/research_workbench/research_workbench_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.parse("roll_mod:textures/gui/research_workbench/research_workbench_gui_progress.png");

    public ResearchWorkbenchScreen(ResearchWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(guiGraphics, x, y);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            // Arrow sits between catalyst slot (x+80) and output slot (x+116)
            guiGraphics.blit(ARROW_TEXTURE, x + 46, y + 18, 0, 0, menu.getScaledProgress(), 42, 68, 42);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
