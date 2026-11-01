package com.roll_54.roll_mod.mixin.client;

import com.roll_54.roll_mod.minestar.chat.client.EmojiPicker;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Ends the chat input line's background where the input now ends, so the emoji square
 * ({@link EmojiPicker}) after it stands as a box of its own rather than over the line.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenEmojiBoxMixin {

    @ModifyArg(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0),
            index = 2)
    private int roll_mod$shortenInputBackground(int right) {
        return right - EmojiPicker.RESERVED;
    }
}
