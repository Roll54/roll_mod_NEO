package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether the loom is showing its pattern list — the skin darkens the list's area while it is not. */
@Mixin(LoomScreen.class)
public interface LoomScreenAccessor {
    @Accessor("displayPatterns")
    boolean roll_mod$displayPatterns();
}
