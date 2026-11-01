package com.roll_54.roll_mod.mixin.client;

import com.roll_54.roll_mod.minestar.chat.client.ClientChatStore;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lines added straight to the chat window — by mods (Modern Industrialization's update notice, FTB's
 * greeting) or by the game itself — go through {@link ClientChatStore} like received messages do, so
 * they are sorted into the chat tabs instead of showing in whatever tab is open.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentStoreMixin {

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void roll_mod$throughStore(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        if (ClientChatStore.intercept(message)) ci.cancel();
    }
}
