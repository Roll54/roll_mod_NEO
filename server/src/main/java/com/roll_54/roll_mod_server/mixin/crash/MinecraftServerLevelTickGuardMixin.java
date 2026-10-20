package com.roll_54.roll_mod_server.mixin.crash;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod_server.crash.CrashReporter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BooleanSupplier;

/**
 * Per-level guard inside {@code tickChildren}: wraps the level tick pre-event, {@code ServerLevel.tick} and
 * the level tick post-event separately, so one throwing level (or one throwing listener, e.g. a pipe network
 * ticking on {@code LevelTickEvent.Post}) only loses that step. Other levels, connections, the player list and
 * chunk sending still tick. {@link MinecraftServerCrashGuardMixin} stays as the outer fallback for anything
 * thrown outside these calls.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerLevelTickGuardMixin {

    @WrapOperation(
            method = "tickChildren",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/event/EventHooks;fireLevelTickPre(Lnet/minecraft/world/level/Level;Ljava/util/function/BooleanSupplier;)V"))
    private void rollmodserver$guardLevelTickPre(Level level, BooleanSupplier hasTimeLeft, Operation<Void> original) {
        try {
            original.call(level, hasTimeLeft);
        } catch (Throwable t) {
            CrashReporter.report((MinecraftServer) (Object) this, t);
        }
    }

    @WrapOperation(
            method = "tickChildren",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void rollmodserver$guardLevelTick(ServerLevel level, BooleanSupplier hasTimeLeft, Operation<Void> original) {
        try {
            original.call(level, hasTimeLeft);
        } catch (Throwable t) {
            CrashReporter.report((MinecraftServer) (Object) this, t);
        }
    }

    @WrapOperation(
            method = "tickChildren",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/event/EventHooks;fireLevelTickPost(Lnet/minecraft/world/level/Level;Ljava/util/function/BooleanSupplier;)V"))
    private void rollmodserver$guardLevelTickPost(Level level, BooleanSupplier hasTimeLeft, Operation<Void> original) {
        try {
            original.call(level, hasTimeLeft);
        } catch (Throwable t) {
            CrashReporter.report((MinecraftServer) (Object) this, t);
        }
    }
}
