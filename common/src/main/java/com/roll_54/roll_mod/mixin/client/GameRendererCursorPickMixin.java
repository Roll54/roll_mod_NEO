package com.roll_54.roll_mod.mixin.client;

import com.roll_54.roll_mod.minestar.hud.CursorPick;
import com.roll_54.roll_mod.minestar.hud.FreeCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While {@link FreeCursor} is held, what you "look at" is what is under the cursor. */
@Mixin(GameRenderer.class)
public abstract class GameRendererCursorPickMixin {
    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void roll_mod$pickUnderCursor(float partialTicks, CallbackInfo ci) {
        if (FreeCursor.active()) CursorPick.apply(Minecraft.getInstance());
    }
}
