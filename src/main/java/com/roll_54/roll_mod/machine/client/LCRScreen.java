package com.roll_54.roll_mod.machine.client;

import com.roll_54.roll_mod.machine.menu.LCRMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LCRScreen extends AbstractContainerScreen<LCRMenu> {
    private Button modeBtn;

    public LCRScreen(LCRMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 8;
        int y = topPos + 8;

        modeBtn = Button.builder(getModeLabel(menu.mode()), b -> {
            menu.requestCycleMode(); // C→S
        }).pos(x, y).size(110, 20).build();

        addRenderableWidget(modeBtn);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        modeBtn.setMessage(getModeLabel(menu.mode()));
    }

    private Component getModeLabel(int m) {
        return Component.literal("Mode: " + (m == 0 ? "LCR" : "UCR"));
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // TODO: фон (blit текстури), за бажанням
    }
}
