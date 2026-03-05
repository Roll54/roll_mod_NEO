package com.roll_54.roll_mod.screen.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.RMMComponents;
import com.roll_54.roll_mod.init.TagRegistry;
import com.roll_54.roll_mod.modItems.spaceModule.CartridgeData;
import com.roll_54.roll_mod.network.packet.PacketLaunchRocket;
import com.roll_54.roll_mod.screen.menu.RocketControllerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

public class RocketControllerScreen extends AbstractContainerScreen<RocketControllerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/gui/rocket_controller.png");
    private Button flightButton;

    public RocketControllerScreen(RocketControllerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        flightButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.roll_mod.rocket.flight_button"), button -> {
                    ItemStack cartridge = this.menu.getSlot(RocketControllerMenu.CARTRIDGE_SLOT_INDEX).getItem();
                    if (!cartridge.isEmpty()) {
                        PacketDistributor.sendToServer(
                                new PacketLaunchRocket(this.menu.blockEntity.getBlockPos())
                        );
                    }
                })
                .bounds(this.leftPos + 60, this.topPos + 50, 50, 20)
                .build()
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        boolean hoveringButton =
                mouseX >= flightButton.getX() &&
                mouseX <= flightButton.getX() + flightButton.getWidth() &&
                mouseY >= flightButton.getY() &&
                mouseY <= flightButton.getY() + flightButton.getHeight();

        if (hoveringButton) {
            List<Component> reason = getBlockReason();
            if (reason != null) {
                guiGraphics.renderTooltip(this.font, reason, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        flightButton.active = getBlockReason() == null;
    }

    /** Returns null if launch is allowed, or a Component describing why it is blocked. */
    private List<Component> getBlockReason() {
        ItemStack rocket    = this.menu.getSlot(RocketControllerMenu.CARTRIDGE_SLOT_INDEX - 2).getItem();
        ItemStack fuel      = this.menu.getSlot(RocketControllerMenu.CARTRIDGE_SLOT_INDEX - 1).getItem();
        ItemStack cartridge = this.menu.getSlot(RocketControllerMenu.CARTRIDGE_SLOT_INDEX).getItem();

        if (rocket.isEmpty() || !rocket.is(TagRegistry.ROCKET_ITEM)) {
            return List.of(Component.translatable("gui.roll_mod.rocket.no_rocket"));
        }

        if (cartridge.isEmpty() || !cartridge.has(RMMComponents.CARTRIDGE_DATA.get())) {
            return List.of(Component.translatable("gui.roll_mod.rocket.no_cartridge"));
        }

        int rocketTier = rocket.getOrDefault(RMMComponents.ROCKET_TIER.get(), 1);
        CartridgeData data = cartridge.get(RMMComponents.CARTRIDGE_DATA.get());

        if (data != null && rocketTier < data.requiredTier()) {
            return List.of(
                    Component.translatable("gui.roll_mod.rocket.tier_too_low")
                            .withStyle(ChatFormatting.RED),

                    Component.translatable("gui.roll_mod.rocket.cartridge_requires", data.requiredTier())
                            .withStyle(ChatFormatting.GOLD),

                    Component.translatable("gui.roll_mod.rocket.your_rocket_tier", rocketTier)
                            .withStyle(ChatFormatting.YELLOW)
            );
        }

        if (fuel.isEmpty() || !fuel.is(TagRegistry.ROCKET_FUEL)) {
            return List.of(Component.translatable("gui.roll_mod.rocket.no_fuel"));
        }

        int required = 5 * rocketTier;
        if (fuel.getCount() < required) {
            return List.of(Component.translatable("gui.roll_mod.rocket.not_enough_fuel", required, fuel.getCount()));
        }

        return null;
    }
}
