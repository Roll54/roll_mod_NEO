package com.roll_54.roll_mod.mixin;

import com.roll_54.roll_mod.util.CommandBlockLogger;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Logs every minecart command block activation to minecraft_root/minestar/logs/commandBlockUsed.txt
 * Uses CommandBlockLogger utility class for all logging operations.
 *
 * @author roll_54
 */
@Mixin(MinecartCommandBlock.class)
public abstract class MinecartCommandBlockLoggingMixin {

    @Shadow
    private BaseCommandBlock commandBlock;

    @Inject(method = "activateMinecart", at = @At("HEAD"))
    private void roll_mod$logMinecartCommandBlockExecution(int x, int y, int z, boolean receivingPower, CallbackInfo ci) {
        MinecartCommandBlock self = (MinecartCommandBlock) (Object) this;

        if (self.level().isClientSide || !receivingPower) {
            return;
        }

        try {
            Vec3 pos = self.position();
            CommandBlockLogger.logMinecartCommand(
                    self.level(),
                    pos.x,
                    pos.y,
                    pos.z,
                    this.commandBlock.getCommand()
            );
        } catch (Exception e) {
            System.err.println("[MinecartCommandBlockLoggingMixin] Failed to log minecart command block: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
