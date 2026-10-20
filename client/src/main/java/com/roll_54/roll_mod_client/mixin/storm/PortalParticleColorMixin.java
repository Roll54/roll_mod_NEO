package com.roll_54.roll_mod_client.mixin.storm;

import com.roll_54.roll_mod.netherstorm.ClientStormCache;
import com.roll_54.roll_mod_client.client.storm.StormPortalClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tints nether portal particles red while the storm is active. */
@Mixin(PortalParticle.class)
public abstract class PortalParticleColorMixin extends TextureSheetParticle {

    protected PortalParticleColorMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void roll_mod$stormTint(ClientLevel level, double x, double y, double z,
                                    double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (!StormPortalClient.spawningPortalParticles || !ClientStormCache.isActive()) return;
        // Vanilla rolls brightness into bCol; keep that variation, just move it onto red.
        float f = this.bCol;
        this.rCol = f;
        this.gCol = f * 0.15F;
        this.bCol = f * 0.1F;
    }
}
