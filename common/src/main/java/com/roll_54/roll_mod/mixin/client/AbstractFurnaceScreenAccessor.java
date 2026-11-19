package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A furnace screen's flame and arrow sprites (furnace, blast furnace and smoker each have their own). */
@Mixin(AbstractFurnaceScreen.class)
public interface AbstractFurnaceScreenAccessor {
    @Accessor("litProgressSprite")
    ResourceLocation roll_mod$litProgressSprite();

    @Accessor("burnProgressSprite")
    ResourceLocation roll_mod$burnProgressSprite();
}
