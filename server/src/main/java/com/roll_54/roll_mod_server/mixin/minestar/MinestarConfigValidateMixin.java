package com.roll_54.roll_mod_server.mixin.minestar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Minestar mod's {@code MinestarConfig#validate()} (a configlib @PostProcess hook) throws
 * "Define serverId in the configuration to an actual server id!" during mod construction whenever the
 * config's serverId is left unset, which aborts the whole dedicated-server launch. This mixin cancels
 * that validation at HEAD so the mod loads with whatever config is present. Targeted by string since
 * Minestar is a bundled third-party jar.
 */
@Mixin(targets = "ua.com.minestar.MinestarConfig", remap = false)
public class MinestarConfigValidateMixin {

    @Inject(method = "validate", at = @At("HEAD"), cancellable = true, remap = false)
    private void rollmodserver$skipServerIdValidation(CallbackInfo ci) {
        ci.cancel();
    }
}
