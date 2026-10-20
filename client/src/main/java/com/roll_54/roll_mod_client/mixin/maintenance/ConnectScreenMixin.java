package com.roll_54.roll_mod_client.mixin.maintenance;

import com.roll_54.roll_mod.minestar.moderation.MaintenanceStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Does not even connect to a server whose last ping said it is on maintenance — the player sees why
 * right away instead of loading in to be kicked. Staff hold Shift while pressing Join to go anyway;
 * the server still decides whether they may stay.
 *
 * <p>Only a pinged server carries a MOTD, so direct connect falls through to the server's own gate.
 */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {

    @Inject(method = "startConnecting", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$maintenanceLock(Screen parent, Minecraft minecraft, ServerAddress address,
                                                 ServerData data, boolean quickPlay,
                                                 @Nullable TransferState transfer, CallbackInfo ci) {
        if (data == null || !MaintenanceStatus.isMaintenance(data.motd) || Screen.hasShiftDown()) return;
        ci.cancel();
        minecraft.setScreen(new DisconnectedScreen(parent,
                Component.translatable("gui.roll_mod.maintenance.title").withStyle(ChatFormatting.YELLOW),
                Component.translatable("gui.roll_mod.maintenance.text")));
    }
}
