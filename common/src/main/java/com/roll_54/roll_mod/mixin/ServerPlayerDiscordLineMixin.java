package com.roll_54.roll_mod.mixin;

import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannels;
import com.roll_54.roll_mod.minestar.chat.ChatHistory;
import com.roll_54.roll_mod.minestar.chat.emoji.EmojiRegistry;
import com.roll_54.roll_mod.registry.ModConfigs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * Lines from the Discord bridge (plain system messages starting with
 * {@code chat_changes.discordLineMarker}) go out as Global channel lines instead, with their Discord
 * emojis and links drawn ({@link EmojiRegistry#render}). Every system message a player gets passes
 * through here, whether the bridge broadcasts it or sends it player by player.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDiscordLineMixin {

    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void rollmod$discordLine(Component message, boolean overlay, CallbackInfo ci) {
        if (overlay || ModConfigs.MAIN == null) return;
        String marker = ModConfigs.MAIN.chat_changes.discordLineMarker;
        if (marker == null || marker.isBlank()) return;
        String plain = message.getString().stripLeading().toLowerCase(Locale.ROOT);
        if (!plain.startsWith(marker.toLowerCase(Locale.ROOT))) return;
        ci.cancel();
        Component line = EmojiRegistry.render(message);
        ChatChannels.deliver((ServerPlayer) (Object) this, ChatChannel.GLOBAL, line);
        // For players joining later; kept once however many players the bridge sends it to.
        ChatHistory.add(ChatChannel.GLOBAL, line, null);
    }
}
