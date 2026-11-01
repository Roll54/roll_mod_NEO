package com.roll_54.roll_mod.mixin.client;

import com.roll_54.roll_mod.minestar.chat.client.EmojiFont;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Ported from Eden_LD (MIT, (c) 2026 LeDok; see docs/third-party-licenses.txt).
 *
 * <p>Keeps the runtime emoji font ({@link EmojiFont}) registered: a resource reload rebuilds every
 * font set from resource packs, so the emoji set is put back right after.
 */
@Mixin(FontManager.class)
public abstract class FontManagerEmojiMixin implements EmojiFont.FontSets {

    @Shadow
    private Map<ResourceLocation, FontSet> fontSets;

    @Override
    public Map<ResourceLocation, FontSet> roll_mod$fontSets() {
        return fontSets;
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void roll_mod$restoreEmoji(CallbackInfo ci) {
        EmojiFont.install();
    }
}
