package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The cursor position vanilla keeps, so {@code FreeCursor} can put it back where the player left it. */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void roll_mod$setXpos(double x);

    @Accessor("ypos")
    void roll_mod$setYpos(double y);
}
