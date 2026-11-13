package com.roll_54.roll_mod_client.compat.tei.mixin;

import com.roll_54.roll_mod.minestar.hub.gui.LdlibScreenBounds;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * TEI asks every plugin for a screen's exclusion areas every frame, and for an LdLib screen (a kit window, a machine)
 * LdLib's plugin walks the whole widget tree to answer — with dozens of slots that is what made those windows lag.
 * For LdLib screens the answer is kept for {@value #KEEP_MILLIS} ms (per screen and size): the areas only move when
 * the window does.
 */
@Mixin(targets = "dev.lorp.tei.client.ExclusionAreas", remap = false)
public abstract class TeiExclusionCacheMixin {

    @Unique private static final long KEEP_MILLIS = 250L;
    @Unique private static Screen roll_mod$screen;
    @Unique private static int roll_mod$w;
    @Unique private static int roll_mod$h;
    @Unique private static long roll_mod$at;
    @Unique private static List<?> roll_mod$cached;

    @Inject(method = "of(Lnet/minecraft/client/gui/screens/Screen;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void roll_mod$cached(Screen screen, CallbackInfoReturnable<List> cir) {
        if (roll_mod$cached != null && screen == roll_mod$screen && screen.width == roll_mod$w
                && screen.height == roll_mod$h && System.currentTimeMillis() - roll_mod$at < KEEP_MILLIS) {
            cir.setReturnValue((List) roll_mod$cached);
        }
    }

    @Inject(method = "of(Lnet/minecraft/client/gui/screens/Screen;)Ljava/util/List;", at = @At("RETURN"))
    @SuppressWarnings("rawtypes")
    private static void roll_mod$keep(Screen screen, CallbackInfoReturnable<List> cir) {
        if (!LdlibScreenBounds.isLdlib(screen)) {
            roll_mod$cached = null;
            return;
        }
        if (roll_mod$cached != null && screen == roll_mod$screen
                && System.currentTimeMillis() - roll_mod$at < KEEP_MILLIS) return;
        roll_mod$screen = screen;
        roll_mod$w = screen.width;
        roll_mod$h = screen.height;
        roll_mod$at = System.currentTimeMillis();
        roll_mod$cached = List.copyOf(cir.getReturnValue());
    }
}
