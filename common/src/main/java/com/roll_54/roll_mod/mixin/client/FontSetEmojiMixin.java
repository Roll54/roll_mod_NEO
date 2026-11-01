package com.roll_54.roll_mod.mixin.client;

import com.mojang.blaze3d.font.GlyphInfo;
import com.roll_54.roll_mod.minestar.chat.client.EmojiFont;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ported from Eden_LD (MIT, (c) 2026 LeDok; see docs/third-party-licenses.txt).
 *
 * <p>An emoji's character draws as the emoji in every font, not only the emoji font, with widths
 * that match what is drawn. Only where the font has no glyph of its own there: a font or resource
 * pack that draws something at that character keeps it. Drawing one also tells {@link EmojiFont}
 * it is on screen (animations pause otherwise).
 */
@Mixin(FontSet.class)
public abstract class FontSetEmojiMixin {

    @Shadow
    private BakedGlyph missingGlyph;

    @Inject(method = "getGlyphInfo", at = @At("RETURN"), cancellable = true)
    private void roll_mod$emojiInfo(int codepoint, boolean filterFishyGlyphs, CallbackInfoReturnable<GlyphInfo> cir) {
        if (cir.getReturnValue() != SpecialGlyphs.MISSING) return;
        FontSet emoji = EmojiFont.borrowingFrom((FontSet) (Object) this, codepoint);
        if (emoji != null) cir.setReturnValue(emoji.getGlyphInfo(codepoint, filterFishyGlyphs));
    }

    @Inject(method = "getGlyph", at = @At("HEAD"))
    private void roll_mod$markDrawn(int codepoint, CallbackInfoReturnable<BakedGlyph> cir) {
        // Text rendering asks for each glyph as it draws it: animated emojis only play while seen.
        EmojiFont.markDrawn(codepoint);
    }

    @Inject(method = "getGlyph", at = @At("RETURN"), cancellable = true)
    private void roll_mod$emojiGlyph(int codepoint, CallbackInfoReturnable<BakedGlyph> cir) {
        if (cir.getReturnValue() != missingGlyph) return;
        FontSet emoji = EmojiFont.borrowingFrom((FontSet) (Object) this, codepoint);
        if (emoji != null) cir.setReturnValue(emoji.getGlyph(codepoint));
    }
}
