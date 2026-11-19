package com.roll_54.roll_mod.mixin.client;

import com.roll_54.roll_mod.minestar.hud.FreeCursor;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mouse buttons while {@link FreeCursor} has the cursor free: handled there, never reaching vanilla —
 * which would otherwise grab the mouse back on the first click, or attack.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerFreeCursorMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void roll_mod$freeCursorClick(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (FreeCursor.onMouseButton(button, action)) ci.cancel();
    }
}
