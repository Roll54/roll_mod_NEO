package com.roll_54.roll_mod_server.mixin.minestar;

import com.roll_54.roll_mod_server.minestar.ChatDelivery;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla chat broadcast for messages that
 * {@link com.roll_54.roll_mod_server.minestar.ChatHandler} already delivered itself.
 *
 * <p>This replaces {@code ServerChatEvent#setCanceled(true)}: cancelling the event stops the
 * NeoForge bus, so every listener after ours stops receiving chat. Dropping the message here
 * instead — after the event has run to completion for all listeners — keeps the routing
 * behaviour identical while leaving the event usable by other mods.
 *
 * <p>Only the {@link ServerPlayer} overload is targeted, which is the one
 * {@code ServerGamePacketListenerImpl#broadcastChatMessage} uses for player chat; the
 * {@code CommandSourceStack} overload behind {@code /say} and friends is untouched. Rate-limit
 * bookkeeping ({@code detectRateSpam}) lives in the caller and still runs.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListChatMixin {

    @Inject(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void roll_mod$skipAlreadyDeliveredChat(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound boundChatType, CallbackInfo ci) {
        if (sender != null && ChatDelivery.consumeHandled(sender)) {
            ci.cancel();
        }
    }
}