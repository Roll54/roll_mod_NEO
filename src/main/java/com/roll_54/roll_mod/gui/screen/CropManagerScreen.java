package com.roll_54.roll_mod.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.roll_54.roll_mod.gui.menu.CropManagerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CropManagerScreen extends AbstractContainerScreen<CropManagerMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.parse("roll_mod:textures/gui/crop_blocks/crop_manager.png"); // using generic texture for now, or fallback if doesn't exist

    public CropManagerScreen(CropManagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // Fallback to a common generic inventory texture if custom texture is not available.
        // Usually, we would use a dedicated texture here. Let's use the generic dispenser or generic 54 if it doesn't exist, but since it's a mod we declare it here.
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }



}
