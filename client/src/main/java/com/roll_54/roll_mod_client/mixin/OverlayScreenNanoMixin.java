package com.roll_54.roll_mod_client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.roll_54.roll_mod.util.NanoSuitUtils;
import earth.terrarium.adastra.api.systems.PlanetData;
import earth.terrarium.adastra.client.config.AdAstraConfigClient;
import earth.terrarium.adastra.client.screens.player.OverlayScreen;
import earth.terrarium.adastra.client.utils.ClientData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds Ad Astra's on-screen HUD bars for a full Extended Industrialization nano suit: an oxygen tank
 * bar (fed by the tank {@code NanoArmorMixin} grafts onto the chestplate) and an energy/battery bar.
 * Ad Astra's {@code OverlayScreen} only draws these for its own {@code SpaceSuitItem}/{@code JetSuitItem},
 * so the nano wearer would otherwise see nothing.
 *
 * <p>Injected at HEAD and self-guarded (re-checking the same player/spectator/debug conditions the
 * original wraps its body in), because the target wraps everything in nested {@code if}s rather than
 * early returns. Space and nano full sets are mutually exclusive, so these never overlap the vanilla
 * bars.
 */
@Mixin(OverlayScreen.class)
public abstract class OverlayScreenNanoMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private static void roll_mod$renderNanoBars(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        if (!NanoSuitUtils.hasFullNanoSet(player)) {
            return;
        }

        Font font = minecraft.font;
        roll_mod$renderOxygenBar(graphics, font, player);
        roll_mod$renderEnergyBar(graphics, font, player);
    }

    // Mirrors OverlayScreen's SpaceSuit oxygen bar, reading the nano chestplate tank via NanoSuitUtils.
    @Unique
    private static void roll_mod$renderOxygenBar(GuiGraphics graphics, Font font, LocalPlayer player) {
        long amount = NanoSuitUtils.chestOxygenAmount(player);
        long capacity = NanoSuitUtils.chestOxygenCapacity(player);
        if (capacity <= 0L) {
            return;
        }

        double ratio = (double) amount / (double) capacity;
        int barHeight = (int) (ratio * 52.0);
        int x = AdAstraConfigClient.oxygenBarX;
        int y = AdAstraConfigClient.oxygenBarY;
        float scale = AdAstraConfigClient.oxygenBarScale;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        graphics.blitSprite(OverlayScreen.OXYGEN_TANK_EMPTY, x, y, 62, 52);
        graphics.blit(OverlayScreen.OXYGEN_TANK, x, y + 52 - barHeight, 0.0F, (float) (52 - barHeight), 62, barHeight, 62, 52);

        String text = String.format("%.1f%%", ratio * 100.0);
        int textWidth = font.width(text);
        int color = ratio <= 0.0 ? 14423100 : 16777215;
        PlanetData localData = ClientData.getLocalData();
        if (localData != null && localData.oxygen()) {
            color = 5635925;
        }
        graphics.drawString(font, text, (int) ((float) x + (float) (62 - textWidth) / 2.0F), y + 52 + 3, color);
        poseStack.popPose();
    }

    // Mirrors OverlayScreen's JetSuit energy bar, but reads nano energy via NanoSuitUtils (grandpower
    // ISimpleEnergyItem) rather than common_storage_lib's EnergyApi. Reading through the common helper
    // keeps the nano item's tesseract supertypes off the client compile classpath.
    @Unique
    private static void roll_mod$renderEnergyBar(GuiGraphics graphics, Font font, LocalPlayer player) {
        long capacity = NanoSuitUtils.chestEnergyCapacity(player);
        if (capacity <= 0L) {
            return;
        }
        long amount = NanoSuitUtils.chestEnergyAmount(player);

        double ratio = (double) amount / (double) capacity;
        int barWidth = (int) (ratio * 49.0);
        int x = AdAstraConfigClient.energyBarX;
        int y = AdAstraConfigClient.energyBarY;
        float scale = AdAstraConfigClient.energyBarScale;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        graphics.blitSprite(OverlayScreen.BATTERY_EMPTY, x, y, 49, 27);
        graphics.blit(OverlayScreen.BATTERY, x, y, 0.0F, 27.0F, barWidth, 27, 49, 27);

        String text = String.format("%.1f%%", ratio * 100.0);
        int textWidth = font.width(text);
        int color = ratio <= 0.0 ? 14423100 : 5636095;
        graphics.drawString(font, text, (int) ((float) x + (float) (49 - textWidth) / 2.0F), y + 27 + 3, color);
        poseStack.popPose();
    }
}
