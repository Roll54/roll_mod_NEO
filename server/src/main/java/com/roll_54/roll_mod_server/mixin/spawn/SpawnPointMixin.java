package com.roll_54.roll_mod_server.mixin.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces every player to spawn at the exact shared spawn coordinates instead of
 * vanilla's search that snaps to the highest valid block (and randomises within the
 * spawn radius) near world spawn. Result: a single global spawn point with identical
 * coordinates for everyone.
 *
 * <p>In 1.21.1 the constructor does
 * {@code moveTo(adjustSpawnLocation(level, level.getSharedSpawnPos()).getBottomCenter(), ...)},
 * so returning the untouched input {@code pos} (the shared spawn pos) skips all adjustment.
 */
@Mixin(ServerPlayer.class)
public abstract class SpawnPointMixin {

    @Inject(method = "adjustSpawnLocation", at = @At("HEAD"), cancellable = true)
    private void roll_mod$exactSharedSpawn(ServerLevel level, BlockPos pos, CallbackInfoReturnable<BlockPos> cir) {
        cir.setReturnValue(pos);
    }
}
