package com.roll_54.roll_mod.mixin;

import com.roll_54.roll_mod.registry.ModConfigs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

/**
 * Advancement announcements fire no cancellable event, but they are sent through this method, so
 * this is where {@code chat_changes.hideAdvancementMessages} drops them.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListBroadcastMixin {

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void rollmod$hideAdvancements(Component message, Function<ServerPlayer, Component> factory,
                                          boolean bypassHiddenChat, CallbackInfo ci) {
        if (ModConfigs.MAIN.chat_changes.hideAdvancementMessages
                && message.getContents() instanceof TranslatableContents tc
                && tc.getKey().startsWith("chat.type.advancement.")) {
            ci.cancel();
        }
    }
}
