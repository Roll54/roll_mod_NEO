package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An image button's sprites — how {@code NoRecipeBook} recognises vanilla's recipe-book button. */
@Mixin(ImageButton.class)
public interface ImageButtonAccessor {
    @Accessor("sprites")
    WidgetSprites roll_mod$sprites();
}
