package com.roll_54.roll_mod.mixin;

import com.roll_54.roll_mod.minestar.moderation.MaintenanceStatus;
import com.roll_54.roll_mod.minestar.moderation.WhitelistStore;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps the server-list MOTD for {@link MaintenanceStatus#motd()} while the whitelist is on. The
 * status is rebuilt every few seconds, so toggling the whitelist shows up in the list shortly after.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerStatusMixin {

    @Inject(method = "buildServerStatus", at = @At("RETURN"), cancellable = true)
    private void roll_mod$maintenanceMotd(CallbackInfoReturnable<ServerStatus> cir) {
        if (!((MinecraftServer) (Object) this).isDedicatedServer() || !WhitelistStore.enabled()) return;
        ServerStatus status = cir.getReturnValue();
        cir.setReturnValue(new ServerStatus(MaintenanceStatus.motd(), status.players(), status.version(),
                status.favicon(), status.enforcesSecureChat(), status.isModded()));
    }
}
