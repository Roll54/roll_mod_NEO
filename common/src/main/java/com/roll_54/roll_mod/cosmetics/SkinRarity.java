package com.roll_54.roll_mod.cosmetics;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** How rare a cosmetic skin is: shown in its colour in the hub and the case opening, and one of the picker's orders. */
public enum SkinRarity {
    COMMON(0xB0C3D9),
    UNCOMMON(0x5E98D9),
    RARE(0x4B69FF),
    EPIC(0xD32CE6),
    LEGENDARY(0xE4AE39);

    /** RGB. */
    public final int color;

    SkinRarity(int color) {
        this.color = color;
    }

    public MutableComponent title() {
        return Component.translatable("skin.roll_mod.rarity." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
