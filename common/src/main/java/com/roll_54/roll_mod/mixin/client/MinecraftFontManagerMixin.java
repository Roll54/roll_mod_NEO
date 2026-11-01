package com.roll_54.roll_mod.mixin.client;

import com.roll_54.roll_mod.minestar.chat.client.EmojiFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Reaches the game's FontManager for the runtime emoji font ({@link EmojiFont}). */
@Mixin(Minecraft.class)
public abstract class MinecraftFontManagerMixin implements EmojiFont.MinecraftFonts {

    @Shadow
    @Final
    private FontManager fontManager;

    @Override
    public FontManager roll_mod$fontManager() {
        return fontManager;
    }
}
