package com.roll_54.roll_mod.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.roll_54.roll_mod.minestar.chat.client.EmojiFont;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Ported from Eden_LD (MIT, (c) 2026 LeDok; see docs/third-party-licenses.txt).
 *
 * <p>Emojis keep their own colors in any text: Minecraft multiplies every glyph by the text color,
 * so an emoji in colored or dark text came out tinted. Their glyphs are drawn white; shadows still
 * dim as usual.
 */
@Mixin(targets = "net.minecraft.client.gui.Font$StringRenderOutput")
public abstract class FontRenderOutputEmojiMixin {

    @ModifyVariable(method = "accept", at = @At("HEAD"), argsOnly = true)
    private Style roll_mod$untintedEmoji(Style style, @Local(argsOnly = true, ordinal = 1) int codepoint) {
        return EmojiFont.isEmoji(codepoint) ? style.withColor(0xFFFFFF) : style;
    }
}
