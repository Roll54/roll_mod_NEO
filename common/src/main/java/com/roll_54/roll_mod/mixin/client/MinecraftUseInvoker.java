package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's right-click use, run by {@code FreeCursor} against the cursor's target. */
@Mixin(Minecraft.class)
public interface MinecraftUseInvoker {
    @Invoker("startUseItem")
    void roll_mod$startUseItem();
}
