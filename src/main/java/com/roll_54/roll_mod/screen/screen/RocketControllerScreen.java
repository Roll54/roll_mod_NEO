package com.roll_54.roll_mod.screen.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.network.packet.PacketLaunchRocket;
import com.roll_54.roll_mod.screen.menu.RocketControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class RocketControllerScreen extends AbstractContainerScreen<RocketControllerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/gui/rocket_controller.png");

    public RocketControllerScreen(RocketControllerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("Flight"), button -> {
            PacketDistributor.sendToServer(new PacketLaunchRocket(this.menu.blockEntity.getBlockPos()));
        })
        .bounds(this.leftPos + 60, this.topPos + 50, 50, 20)
        .build());
    }

    @Override
    protected void renderBg(GuiGraphics guigraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = this.leftPos;
        int j = this.topPos;
        guigraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guigraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guigraphics, mouseX, mouseY, partialTick);
        super.render(guigraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guigraphics, mouseX, mouseY);
    }
}
