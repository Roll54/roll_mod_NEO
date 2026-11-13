package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The screen vanilla's server list returns to — read when {@code MultiplayerScreenReplacer} swaps
 * the list for the LdLib one, so Back still leads where the caller meant it to.
 */
@Mixin(JoinMultiplayerScreen.class)
public interface JoinMultiplayerScreenAccessor {
    @Accessor("lastScreen")
    Screen roll_mod$getLastScreen();
}
