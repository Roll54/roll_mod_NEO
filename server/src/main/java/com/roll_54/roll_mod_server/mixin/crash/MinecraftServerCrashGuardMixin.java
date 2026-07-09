package com.roll_54.roll_mod_server.mixin.crash;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod_server.crash.CrashReporter;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BooleanSupplier;

/**
 * Wraps the {@code this.tickServer(...)} call inside the dedicated server's {@code runServer} loop so a
 * runtime exception during ticking (world/entity ticks, command execution, event handlers on the
 * server thread) is caught and reported to ops in chat via {@link CrashReporter} instead of bubbling
 * up to vanilla's crash handling and killing the server. The failing tick is skipped and the loop
 * continues.
 *
 * <p>Uses {@code @WrapOperation} (MixinExtras 0.3.5 has no {@code @WrapMethod}). Only covers exceptions
 * on the main server thread — not worldgen/network worker threads or startup/shutdown.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerCrashGuardMixin {

    @WrapOperation(
            method = "runServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;tickServer(Ljava/util/function/BooleanSupplier;)V"))
    private void rollmodserver$guardTick(MinecraftServer instance, BooleanSupplier hasTimeLeft, Operation<Void> original) {
        try {
            original.call(instance, hasTimeLeft);
        } catch (Throwable t) {
            CrashReporter.report(instance, t);
        }
    }
}
