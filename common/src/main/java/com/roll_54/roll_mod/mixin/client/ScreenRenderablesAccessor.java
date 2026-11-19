package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** A screen's draw list — so a listener NeoForge's init event added as renderable can be taken back out of it. */
@Mixin(Screen.class)
public interface ScreenRenderablesAccessor {
    @Accessor("renderables")
    List<Renderable> roll_mod$renderables();
}
